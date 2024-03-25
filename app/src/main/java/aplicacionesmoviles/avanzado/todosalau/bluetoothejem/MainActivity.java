package aplicacionesmoviles.avanzado.todosalau.bluetoothejem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<String> devicesList;

    // ActivityResultLauncher para el inicio de la actividad de habilitación de Bluetooth
    private ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Bluetooth habilitado, iniciar búsqueda de dispositivos
                    searchBluetoothDevices();
                } else {
                    // El usuario no habilitó Bluetooth, mostrar mensaje o tomar acción adicional si es necesario
                    Toast.makeText(this, "El usuario no habilitó Bluetooth", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // El dispositivo no admite Bluetooth
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar ListView y Adapter
        ListView listView = findViewById(R.id.listView);
        devicesList = new ArrayList<>();
        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devicesList);
        listView.setAdapter(devicesAdapter);

        // Botón para buscar dispositivos Bluetooth
        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> searchBluetoothDevices());

        // Registrar BroadcastReceiver para descubrimiento de dispositivos Bluetooth
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detener descubrimiento y liberar recursos
        if (bluetoothAdapter != null) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
        unregisterReceiver(receiver);
    }

    // BroadcastReceiver para manejar el descubrimiento de dispositivos Bluetooth
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Un dispositivo Bluetooth ha sido encontrado
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    // Agregar dispositivo a la lista
                    String deviceName = device.getName() != null ? device.getName() : "Dispositivo desconocido";
                    String deviceAddress = device.getAddress();
                    String deviceInfo = deviceName + "\n" + deviceAddress;
                    devicesList.add(deviceInfo);
                    devicesAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // Método para buscar dispositivos Bluetooth
    private void searchBluetoothDevices() {
        // Verificar si el Bluetooth está habilitado
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent); // Lanzar la actividad para habilitar Bluetooth
        } else {
            // Limpiar la lista de dispositivos y comenzar la búsqueda
            devicesList.clear();
            devicesAdapter.notifyDataSetChanged();
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // El permiso no está otorgado, solicitar permiso en tiempo de ejecución
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            } else {
                // El permiso está otorgado, comenzar descubrimiento
                bluetoothAdapter.startDiscovery();
            }
        }
    }

    private static final int REQUEST_FINE_LOCATION_PERMISSION = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_FINE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, comenzar descubrimiento
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothAdapter.startDiscovery();
            } else {
                // Permiso denegado, mostrar mensaje o tomar alguna acción adicional
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
