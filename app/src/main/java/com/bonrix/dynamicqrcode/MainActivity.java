package com.bonrix.dynamicqrcode;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import com.bonrix.dynamicqrcode.permissionutils.AskagainCallback;
import com.bonrix.dynamicqrcode.permissionutils.FullCallback;
import com.bonrix.dynamicqrcode.permissionutils.PermissionEnum;
import com.bonrix.dynamicqrcode.permissionutils.PermissionManager;
import com.bonrix.dynamicqrcode.permissionutils.PermissionUtils;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener, FullCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new NewHomeFragment(), "devices").commit();
        } else {
            onBackStackChanged();
        }
        askRequiredPermissions();
    }

    private void askRequiredPermissions() {
        if (
                PermissionUtils.isGranted(MainActivity.this, PermissionEnum.WRITE_EXTERNAL_STORAGE)
                        && PermissionUtils.isGranted(MainActivity.this, PermissionEnum.READ_EXTERNAL_STORAGE)

        ) {
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ArrayList<PermissionEnum> permissions = new ArrayList<>();
                permissions.add(PermissionEnum.WRITE_EXTERNAL_STORAGE);
                permissions.add(PermissionEnum.READ_EXTERNAL_STORAGE);


                PermissionManager.with(MainActivity.this)
                        .permissions(permissions)
                        .askagain(true)
                        .askagainCallback(new AskagainCallback() {
                            @Override
                            public void showRequestPermission(UserResponse response) {
                                showDialog(response);
                            }
                        })
                        .callback(this).ask();
            } else {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handleResult(requestCode, permissions, grantResults);
        if (
                PermissionUtils.isGranted(MainActivity.this, PermissionEnum.WRITE_EXTERNAL_STORAGE)
                        && PermissionUtils.isGranted(MainActivity.this, PermissionEnum.READ_EXTERNAL_STORAGE)
        ) {
        } else {
        }
    }

    private void showDialog(final AskagainCallback.UserResponse response) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Permission Request")
                .setMessage("Application really needs this permission to run all function properly, Allow this permission?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        response.result(true);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        response.result(false);
                    }
                })
                .show();
    }

    @Override
    public void result(ArrayList<PermissionEnum> permissionsGranted, ArrayList<PermissionEnum> permissionsDenied, ArrayList<PermissionEnum> permissionsDeniedForever, ArrayList<PermissionEnum> permissionsAsked) {
        boolean isAsked = !permissionsAsked.isEmpty();
        boolean isDenied = !permissionsDenied.isEmpty();
        String message;
        if (isAsked && isDenied) {
            message = "You haven't allowed all permission requested by " + getString(R.string.app_name);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission Request")
                    .setMessage(message)
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            askRequiredPermissions();
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            NewHomeFragment terminal = (NewHomeFragment) getSupportFragmentManager().findFragmentByTag("terminal");
            if (terminal != null)
                terminal.status("USB device detected");
        }
        super.onNewIntent(intent);
    }
}
