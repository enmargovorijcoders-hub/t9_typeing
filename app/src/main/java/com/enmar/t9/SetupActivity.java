package com.enmar.t9;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SetupActivity extends Activity {

    private Button makeGreenButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.parseColor("#E8F5E9"));
        b.setTextSize(14f);
        int padV = (int) (12 * getResources().getDisplayMetrics().density);
        int padH = (int) (14 * getResources().getDisplayMetrics().density);
        b.setPadding(padH, padV, padH, padV);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(primary ? "#2E7D32" : "#162B1E"));
        gd.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor(primary ? "#4CAF50" : "#264D34"));
        gd.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        b.setBackgroundDrawable(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0D1A12"));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("T9 Multi-Tap Keyboard");
        title.setTextColor(Color.parseColor("#A5D6A7"));
        title.setTextSize(20f);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("1. Enable T9 Keyboard in Android Settings.\n2. Switch input method to T9 Keyboard.\n3. Test typing below.");
        desc.setTextColor(Color.parseColor("#81C784"));
        desc.setTextSize(14f);
        desc.setPadding(0, pad / 2, 0, pad);
        layout.addView(desc);

        Button btnEnable = makeGreenButton("1. Enable Keyboard in Settings", false);
        btnEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        layout.addView(btnEnable);

        Button btnSelect = makeGreenButton("2. Select T9 Keyboard", true);
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
        testBox.setTextColor(Color.parseColor("#E8F5E9"));
        testBox.setHintTextColor(Color.parseColor("#558B65"));
        testBox.setPadding(pad, pad, pad, pad);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.parseColor("#162B1E"));
        boxBg.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#264D34"));
        boxBg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        testBox.setBackgroundDrawable(boxBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = pad / 2;
        testBox.setLayoutParams(lp);
        layout.addView(testBox);

        setContentView(layout);
    }
}
