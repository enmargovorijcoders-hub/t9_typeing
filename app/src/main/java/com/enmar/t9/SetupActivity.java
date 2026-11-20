package com.enmar.t9;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#121212"));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("T9 Multi-Tap Keyboard");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("1. Enable T9 Keyboard in Android Settings.\n2. Switch input method to T9 Keyboard.\n3. Test typing below.");
        desc.setTextColor(Color.LTGRAY);
        desc.setTextSize(14f);
        desc.setPadding(0, pad / 2, 0, pad);
        layout.addView(desc);

        Button btnEnable = new Button(this);
        btnEnable.setText("1. Enable Keyboard in Settings");
        btnEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        layout.addView(btnEnable);

        Button btnSelect = new Button(this);
        btnSelect.setText("2. Select T9 Keyboard");
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });
        layout.addView(btnSelect);

        EditText testBox = new EditText(this);
        testBox.setHint("Tap here to test T9 typing...");
        testBox.setTextColor(Color.WHITE);
        testBox.setHintTextColor(Color.GRAY);
        testBox.setBackgroundColor(Color.parseColor("#242424"));
        testBox.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = pad;
        testBox.setLayoutParams(lp);
        layout.addView(testBox);

        setContentView(layout);
    }
}
