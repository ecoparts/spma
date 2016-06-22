package de.dralle.bluetoothtest.BGS;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.UUID;

import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 31.05.16.
 * SPMAService is the background service of this app TODO: maybe merge liistener classes with connection classes through inheritance
 */
public class SPMAService extends IntentService {

    /**
     * Local broadcast tag. Used to receive internal messages
     */
    public static final String ACTION_NEW_MSG = "SPMAService.ACTION_NEW_MSG";
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMAService.class.getName();
    /**
     * Selected local user
     */
    private LocalUserManager localUserManager = null;
    /**
     * Class to help with checking UUIDs
     */
    private UUIDChecker uuidChecker = null;
    /**
     * Help with device handling
     */
    private RemoteBTDeviceManager deviceManager = null;
    /**
     *
     */
    private SPMAServiceBroadcastReceiver spmaBroadcastReceiver = null;
    /**
     * Send internal messages.
     */
    private InternalMessageSender internalMessageSender = null;
    /**
     * Manages Bluetooth functions
     */
    private LocalBluetoothManager bluetoothManager = null;
    /**
     * for parsing internal messages
     */
    private InternalMessageParser internalMessageParser = null;
    /**
     * Encryption
     */
    private Encryption enc = null;
    /**
     * External message sender
     */
    private ExternalMessageSender externalMessageSender = null;
    /**
     * Parse external messages
     */
    private ExternalMessageParser externalMessageParser = null;
    /**
     * Handle main service notification
     */
    private SPMANotificationManager notificationManager = null;

    /**
     * Constructor. Gives a name this service. Initializes connection observer
     */
    public SPMAService() {
        super("SPMAService");

        spmaBroadcastReceiver = new SPMAServiceBroadcastReceiver();
        uuidChecker = new UUIDChecker(true);
        spmaBroadcastReceiver.setUuidChecker(uuidChecker);

        internalMessageSender = new InternalMessageSender(this);//prepare broadcast sender
        spmaBroadcastReceiver.setInternalMessageSender(internalMessageSender);
        deviceManager = new RemoteBTDeviceManager();
        deviceManager.setContext(this);
        spmaBroadcastReceiver.setDeviceManager(deviceManager);
        bluetoothManager = new LocalBluetoothManager();
        bluetoothManager.setContext(this);
        bluetoothManager.setDeviceManager(deviceManager);
        internalMessageParser = new InternalMessageParser(this);
        internalMessageParser.setBluetoothManager(bluetoothManager);
        internalMessageParser.setDeviceManager(deviceManager);
        internalMessageParser.setInternalMessageSender(internalMessageSender);
        localUserManager = new LocalUserManager();
        localUserManager.setContext(this);
        localUserManager.setBluetoothManager(bluetoothManager);
        localUserManager.setInternalMessageSender(internalMessageSender);
        localUserManager.setDeviceManager(deviceManager);
        enc = new Encryption();
        enc.setContext(this);
        internalMessageParser.setEnc(enc);
        internalMessageParser.setLocalUserManager(localUserManager);
        externalMessageSender = new ExternalMessageSender(this);
        externalMessageSender.setLocalUserManager(localUserManager);
        externalMessageSender.setEnc(enc);
        externalMessageParser = new ExternalMessageParser(this);
        externalMessageParser.setEnc(enc);
        externalMessageParser.setLocalUserManager(localUserManager);
        externalMessageParser.setDeviceManager(deviceManager);
        externalMessageParser.setInternalMessageSender(internalMessageSender);
        externalMessageParser.setExternalMessageSender(externalMessageSender);
        notificationManager = new SPMANotificationManager(this);
        internalMessageParser.setExternalMessageSender(externalMessageSender);
        internalMessageParser.setExternalMessageParser(externalMessageParser);
        BluetoothConnectionObserver.getInstance().setInternalMessageParser(internalMessageParser); //prepare observer for later use
        BluetoothConnectionObserver.getInstance().setInternalMessageSender(internalMessageSender); //prepare observer for later use
        BluetoothConnectionObserver.getInstance().setDeviceManager(deviceManager);//prepare observer for later use
        BluetoothListenerObserver.getInstance().setInternalMessageParser(internalMessageParser);//prepare observer for later use
    }


    @Override
    protected void onHandleIntent(Intent intent) {


    }

    @Override
    public void onCreate() {
        super.onCreate();

        //register a receiver for broadcasts
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(spmaBroadcastReceiver, filter);

        filter = new IntentFilter(SPMAService.ACTION_NEW_MSG);
        registerReceiver(spmaBroadcastReceiver, filter);

        BluetoothConnectionMaker.getInstance(getResources()); //prepare BluetoothConnectionMaker for later use
        BluetoothListenerMaker.getInstance(getResources());//prepare BluetoothListenerMaker for later use


        SPMADatabaseAccessHelper.getInstance(this); //prepare database for use

        notificationManager.startNotification();

        //Add UUIDs to check for when scanning
        uuidChecker.addUUID(UUID.fromString(getResources().getString(R.string.uuid_secure)));
        uuidChecker.addUUID(UUID.fromString(getResources().getString(R.string.uuid_insecure)));


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(spmaBroadcastReceiver);
        SPMADatabaseAccessHelper.getInstance(this).closeConnections(); //close Database connections
        notificationManager.endNotification();
    }


    /**
     * Called when the service is started.
     *
     * @param intent  The Starting intent
     * @param flags
     * @param startId
     * @return START_STICKY, that android doesn´t remove this
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
