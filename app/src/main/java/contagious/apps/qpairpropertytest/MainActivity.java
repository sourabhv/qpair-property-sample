package contagious.apps.qpairpropertytest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.lge.qpair.api.r1.QPairConstants;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {

    public static final String IS_CONNECTED_PROPERTY_URI = "/local/qpair/is_connected";
    public static final String IS_QPAIR_ON_PROPERTY_URI = "/local/qpair/is_on";

    ContentResolver contentResolver;
    Map<String, String> properties;

    TextView status;
    EditText getPropertyName;
    TextView getPropertyValue;
    EditText setPropertyName;
    EditText setPropertyValue;
    EditText trackPropertyName;
    TextView trackPropertyValue;
    EditText deletePropertyName;

    RadioButton getLocal;
    RadioButton getPeer;
    RadioButton trackLocal;
    RadioButton trackPeer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentResolver = getContentResolver();
        properties = new HashMap<String, String>();

        status = (TextView) findViewById(R.id.status);
        getPropertyName = (EditText) findViewById(R.id.get_property_name);
        getPropertyValue = (TextView) findViewById(R.id.get_property_value);
        setPropertyName = (EditText) findViewById(R.id.set_property_name);
        setPropertyValue = (EditText) findViewById(R.id.set_property_value);
        trackPropertyName = (EditText) findViewById(R.id.track_property_name);
        trackPropertyValue = (TextView) findViewById(R.id.track_property_value);
        deletePropertyName = (EditText) findViewById(R.id.delete_property_value);

        getLocal = (RadioButton) findViewById(R.id.get_local);
        getPeer = (RadioButton) findViewById(R.id.get_peer);
        trackLocal = (RadioButton) findViewById(R.id.track_local);
        trackPeer = (RadioButton) findViewById(R.id.track_peer);

        // check if QPair is connected or not
        boolean qpair_is_on = getQpairProperty(contentResolver,
                QPairConstants.PROPERTY_SCHEME_AUTHORITY + IS_QPAIR_ON_PROPERTY_URI, "false").equals("true");
        boolean qpair_is_connected = getQpairProperty(contentResolver,
                QPairConstants.PROPERTY_SCHEME_AUTHORITY + IS_CONNECTED_PROPERTY_URI, "false").equals("true");
        String message = "";
        if (!qpair_is_on)
            message = "QPair is turned off. Please turn it on and connect to peer.";
        else if (!qpair_is_connected)
            message = "QPair is not connected to peer.";

        if (!message.equals(""))
            new AlertDialog.Builder(this)
                    .setTitle("QPair")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
    }

    @Override
    protected void onPause() {
        StringBuilder pauseString = new StringBuilder();
        pauseString.append("\nIn onPause, ");
        for (Object o : properties.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            deleteQPairProperty(contentResolver, pair.getKey().toString());
            pauseString.append("delete ").append(pair.getKey().toString()).append(", ");
        }
        status.setText(status.getText() + pauseString.toString());
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder resumeString = new StringBuilder();
        resumeString.append("\nIn onResume, ");
        for (Object o : properties.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            setQPairProperty(contentResolver, pair.getKey().toString(), pair.getValue().toString());
            resumeString.append("insert ").append(pair.getKey().toString()).append(": ").append(pair.getValue().toString()).append(", ");
        }
        status.setText(status.getText() + resumeString.toString());
    }

    class QPairContentObserver extends ContentObserver {
        public QPairContentObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Toast.makeText(MainActivity.this, "Inside onChange. uri: " + uri.toString(), Toast.LENGTH_LONG).show();
            String value = getQpairProperty(contentResolver, uri.toString(), "not found");
            trackPropertyValue.setText(value);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Toast.makeText(MainActivity.this, "Oh so this one!", Toast.LENGTH_LONG).show();
        }
    }

    public void getProperty(View view) {
        String property_name = getPropertyName.getText().toString();
        String owner = "";

        if (getLocal.isChecked())
            owner = "/local/"+ getPackageName() + "/";
        else if (getPeer.isChecked())
            owner = "/peer/"+ getPackageName() + "/";

        if (!owner.equals("") && !property_name.equals("")) {
            String uriString = QPairConstants.PROPERTY_SCHEME_AUTHORITY + owner + property_name;
            String value = getQpairProperty(contentResolver, uriString, "<\"" + property_name + "\" not found>");
            getPropertyValue.setText(value);
        }
    }

    public void setProperty(View view) {
        String property_name = setPropertyName.getText().toString();
        String property_value = setPropertyValue.getText().toString();

        if (!property_name.equals("") && !property_value.equals("")) {
            String uriString = QPairConstants.PROPERTY_SCHEME_AUTHORITY + "/local/" +
                    getPackageName() + "/" + property_name;
            try {
                setQPairProperty(contentResolver, uriString, property_value);
            } catch (Exception e) {
                updateQPairProperty(contentResolver, uriString, property_value);
            }
        }
        properties.put(property_name, property_value);
        setPropertyName.setText("");
        setPropertyValue.setText("");
    }

    public void trackProperty(View view) {
        String property_name = trackPropertyName.getText().toString();
        String owner = "";

        if (trackLocal.isChecked())
            owner = "/local/" + getPackageName() + "/";
        else if (trackPeer.isChecked())
            owner = "/peer/" + getPackageName() + "/";

        if (!owner.equals("") && !property_name.equals("")) {
            String uriString = QPairConstants.PROPERTY_SCHEME_AUTHORITY + owner + property_name;
            trackQpairProperty(contentResolver, uriString);
        }
    }

    public void deleteProperty(View view) {
        String property_name = deletePropertyName.getText().toString();
        String owner = "/local/" + getPackageName() + "/";
        String uriString = QPairConstants.PROPERTY_SCHEME_AUTHORITY + owner + property_name;
        deleteQPairProperty(contentResolver, uriString);
    }

    public static String getQpairProperty(ContentResolver resolver, String uriString, String defaultValue) {
        Uri uri = Uri.parse(uriString);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        String value = defaultValue;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    value = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return value;
    }

    public static Uri setQPairProperty(ContentResolver resolver, String uriString, String value) {
        Uri uri = Uri.parse(uriString);
        ContentValues cv = new ContentValues();
        cv.put("", value);
        return resolver.insert(uri, cv);
    }

    public static int updateQPairProperty(ContentResolver resolver, String uriString, String value) {
        Uri uri = Uri.parse(uriString);
        ContentValues cv = new ContentValues();
        cv.put("", value);
        return resolver.update(uri, cv, null, null);
    }

    public static int deleteQPairProperty(ContentResolver resolver, String uriString) {
        Uri uri = Uri.parse(uriString);
        return resolver.delete(uri, null, null);
    }

    private void trackQpairProperty(ContentResolver contentResolver, String uriString) {
        ContentObserver observer = new QPairContentObserver(new Handler());
        Uri uri = Uri.parse(uriString);
        contentResolver.registerContentObserver(uri, false, observer);
    }

}
