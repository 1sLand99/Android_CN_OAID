/*
 * Copyright (c) 2019-2021 gzu-liyujiang <1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 *     http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v1 for more details.
 *
 */
package com.github.gzuliyujiang.oaid.impl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.bun.lib.MsaIdInterface;
import com.github.gzuliyujiang.logger.Logger;
import com.github.gzuliyujiang.oaid.IDeviceId;
import com.github.gzuliyujiang.oaid.IGetter;
import com.github.gzuliyujiang.oaid.IOAIDGetter;

import java.lang.reflect.Method;

/**
 * Created by liyujiang on 2020/5/30
 *
 * @author 大定府羡民（1032694760@qq.com）
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MsaDeviceIdImpl implements IDeviceId {
    private final Context context;

    public MsaDeviceIdImpl(Context context) {
        this.context = context;
    }

    @Override
    public boolean supportOAID() {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo("com.mdid.msa", 0);
            return pi != null;
        } catch (Exception e) {
            Logger.print(e);
            return false;
        }
    }

    @Override
    public void doGet(@NonNull final IOAIDGetter getter) {
        startMsaKlService();
        Intent intent = new Intent("com.bun.msa.action.bindto.service");
        intent.setClassName("com.mdid.msa", "com.mdid.msa.service.MsaIdService");
        intent.putExtra("com.bun.msa.param.pkgname", context.getPackageName());
        try {
            boolean isBinded = context.bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Logger.print("MsaIdService connected");
                    try {
                        //MsaIdInterface anInterface = new MsaIdInterface.Stub.asInterface(service);
                        Method asInterface = MsaIdInterface.Stub.class.getDeclaredMethod("asInterface", IBinder.class);
                        MsaIdInterface anInterface = (MsaIdInterface) asInterface.invoke(null, service);
                        if (anInterface == null) {
                            throw new RuntimeException("MsaIdInterface is null");
                        }
                        String oaid = anInterface.getOAID();
                        if (oaid == null || oaid.length() == 0) {
                            throw new RuntimeException("Msa oaid get failed");
                        }
                        getter.onOAIDGetComplete(oaid);
                    } catch (Exception e) {
                        Logger.print(e);
                        getter.onOAIDGetError(e);
                    } finally {
                        context.unbindService(this);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Logger.print("MsaIdService disconnected");
                }
            }, Context.BIND_AUTO_CREATE);
            if (!isBinded) {
                throw new RuntimeException("MsaIdService bind failed");
            }
        } catch (Exception e) {
            getter.onOAIDGetError(e);
        }
    }

    private void startMsaKlService() {
        try {
            Intent intent = new Intent("com.bun.msa.action.start.service");
            intent.setClassName("com.mdid.msa", "com.mdid.msa.service.MsaKlService");
            intent.putExtra("com.bun.msa.param.pkgname", context.getPackageName());
            intent.putExtra("com.bun.msa.param.runinset", true);
            context.startService(intent);
        } catch (Exception e) {
            Logger.print(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void doGet(@NonNull final IGetter getter) {
        doGet(new IOAIDGetter() {
            @Override
            public void onOAIDGetComplete(@NonNull String oaid) {
                getter.onDeviceIdGetComplete(oaid);
            }

            @Override
            public void onOAIDGetError(@NonNull Exception exception) {
                getter.onDeviceIdGetError(exception);
            }
        });
    }

}
