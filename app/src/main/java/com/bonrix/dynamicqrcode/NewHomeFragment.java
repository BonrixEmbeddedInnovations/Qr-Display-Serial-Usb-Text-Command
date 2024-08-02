package com.bonrix.dynamicqrcode;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;


public class NewHomeFragment extends Fragment implements ServiceConnection, SerialListener {
    private enum Connected {False, Pending, True}

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private SerialService service;
    private TextView receiveText;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private Button btnWelcome, btnGenerateQr, btnSuccess, btnFail, btnPending;

    public NewHomeFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        try {
            deviceId = getArguments().getInt("device");
            portNum = getArguments().getInt("port");
            baudRate = getArguments().getInt("baud");
        } catch (Exception e) {
        }

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False) disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null) service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations()) service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_GRANT_USB));
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newhome, container, false);
        receiveText = view.findViewById(R.id.receive_text);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        btnWelcome = view.findViewById(R.id.btnWelcome);
        btnSuccess = view.findViewById(R.id.btnSuccess);
        btnFail = view.findViewById(R.id.btnFail);
        btnPending = view.findViewById(R.id.btnPending);
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText));
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        btnWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    if (connected.toString().equalsIgnoreCase("False")) {
                        Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                        replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                        return;
                    }
                    String data="WelcomeScreen**";
                    sendNew1(data);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }
        });
        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected.toString().equalsIgnoreCase("False")) {
                    Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                    replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                    return;
                }
                String upistring = "DisplayQRCodeScreen**upi://pay?pa=abc@icici&pn=testuser&cu=INR&am=10&pn=3323231**10**abc@icici";
                if (TextUtils.isEmpty(upistring)) {
                    Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
                } else {
                    sendNew1(upistring);
                }
            }
        });
        btnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected.toString().equalsIgnoreCase("False")) {
                    Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                    replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                    return;
                }
                String data = "DisplaySuccessQRCodeScreen**21312fdfsd**dfsfadsfads**01-08-2024";
                if (TextUtils.isEmpty(data)) {
                    Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
                } else {
                    sendNew1(data);
                }
            }
        });
        btnFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected.toString().equalsIgnoreCase("False")) {
                    Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                    replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                    return;
                }
                String data = "DisplayFailQRCodeScreen**21312fdfsd**dfsfadsfads**01-08-2024";
                if (TextUtils.isEmpty(data)) {
                    Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
                } else {
                    sendNew1(data);
                }
            }
        });
        btnPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected.toString().equalsIgnoreCase("False")) {
                    Toast.makeText(getActivity(), "First Connect USB Device.", Toast.LENGTH_SHORT).show();
                    replaceFragment(new DevicesFragment(), R.id.fragment, DevicesFragment.class.getName());
                    return;
                }
                String data = "DisplayCancelQRCodeScreen**21312fdfsd**dfsfadsfads**01-08-2024";
                if (TextUtils.isEmpty(data)) {
                    Toast.makeText(getActivity(), "Invalid UPI Data", Toast.LENGTH_SHORT).show();
                } else {
                    sendNew1(data);
                }
            }
        });

        return view;
    }

    private void sendNew1(String result) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            try {
                byte[] data = (result + '\n').getBytes();
                service.write(data);

            } catch (Exception e) {
            }

        } catch (Exception e) {
        }
    }

    void replaceFragment(Fragment mFragment, int id, String tag) {
        FragmentTransaction mTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        mTransaction.replace(id, mFragment);
        mTransaction.addToBackStack(mFragment.toString());
        mTransaction.commit();

    }

    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getDeviceId() == deviceId) device = v;
        if (device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(device.getVendorId(), device.getProductId(), CdcAcmSerialDriver.class);
            UsbSerialProber prober = new UsbSerialProber(customTable);
            driver = prober.probeDevice(device);
        }
        if (driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if (usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(Constants.INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else status("connection failed: open failed");
            return;
        }
        connected = Connected.Pending;
        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), usbConnection, usbSerialPort);
            service.connect(socket);
            onSerialConnect();
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        usbSerialPort = null;
    }


    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
