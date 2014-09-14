package com.alexandrumos.v1.mygeolocation.mygeolocation;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;


public class AboutScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.about);

        try {
            InputStream stream = getAssets().open("mit.txt");

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            String license = new String(buffer);

            TextView licenseTextView = (TextView) findViewById(R.id.textView5);
            TextView textView6 = (TextView) findViewById(R.id.textView6);

            licenseTextView.setText(license);

            textView6.setMovementMethod(LinkMovementMethod.getInstance());

        } catch (IOException e) {
            // Handle exceptions here
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's back button
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
