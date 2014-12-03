package contagious.apps.qpairpropertytest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.lge.qpair.api.r1.QPairConstants;


public class MainActivity extends Activity {

    public static final String IS_CONNECTED_PROPERTY_URI = "/local/qpair/is_connected";
    public static final String IS_QPAIR_ON_PROPERTY_URI = "/local/qpair/is_on";

    EditText getPropertyName;
    TextView getPropertyValue;
    EditText setPropertyName;
    EditText setPropertyValue;
    RadioButton local;
    RadioButton peer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPropertyName = (EditText) findViewById(R.id.get_property_name);
        getPropertyValue = (TextView) findViewById(R.id.get_property_value);
        setPropertyName = (EditText) findViewById(R.id.set_property_name);
        setPropertyValue = (EditText) findViewById(R.id.set_property_value);
        local = (RadioButton) findViewById(R.id.local);
        peer = (RadioButton) findViewById(R.id.peer);

        // check if QPair is connected or not
    }

    public void getProperty(View view) {
        String property_name = getPropertyName.getText().toString();
        String owner = "";

        if (local.isChecked())
            owner = "/local/"+ getPackageName() + "/";
        else if (peer.isChecked())
            owner = "/peer/"+ getPackageName() + "/";

        if (!owner.equals("") && !property_name.equals("")) {
            String uriString = QPairConstants.PROPERTY_SCHEME_AUTHORITY + owner + property_name;
            String value = getQpairProperty(getContentResolver(), uriString, "<\"" + property_name + "\" not found>");
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
                setQPairProperty(getContentResolver(), uriString, property_value);
            } catch (Exception e) {
                updateQPairProperty(getContentResolver(), uriString, property_value);
            }
        }

        setPropertyName.setText("");
        setPropertyValue.setText("");
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
    public static int deleteQPairProperty(ContentResolver resolver, String uriString) {
        Uri uri = Uri.parse(uriString);
        return resolver.delete(uri, null, null);
    }

}
