package com.simple.t9keyboard;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

public class T9InputMethodService extends InputMethodService {

    private static final long MULTI_TAP_TIMEOUT_MS = 850L;

    // Mode: 0 = lowercase (abc), 1 = uppercase (ABC), 2 = numbers only (123)
    private int inputMode = 0;

    private int lastKeyId = -1;
    private int tapIndex = 0;
    private boolean hasPendingChar = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable commitRunnable = new Runnable() {
        @Override
        public void run() {
            commitPendingChar();
        }
    };

    private Button btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0, btnStar, btnHash;
    private Button btnDel, btnCase, btnEnter, btnNext;

    @Override
    public View onCreateInputView() {
        View root = getLayoutInflater().inflate(R.layout.keyboard_view, null);

        btn1 = root.findViewById(R.id.key_1);
        btn2 = root.findViewById(R.id.key_2);
        btn3 = root.findViewById(R.id.key_3);
        btn4 = root.findViewById(R.id.key_4);
        btn5 = root.findViewById(R.id.key_5);
        btn6 = root.findViewById(R.id.key_6);
        btn7 = root.findViewById(R.id.key_7);
        btn8 = root.findViewById(R.id.key_8);
        btn9 = root.findViewById(R.id.key_9);
        btn0 = root.findViewById(R.id.key_0);
        btnStar = root.findViewById(R.id.key_star);
        btnHash = root.findViewById(R.id.key_hash);

        btnDel = root.findViewById(R.id.key_del);
        btnCase = root.findViewById(R.id.key_case);
        btnEnter = root.findViewById(R.id.key_enter);
        btnNext = root.findViewById(R.id.key_next);

        View.OnClickListener dialClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMultiTapKey(v.getId());
            }
        };

        btn1.setOnClickListener(dialClickListener);
        btn2.setOnClickListener(dialClickListener);
        btn3.setOnClickListener(dialClickListener);
        btn4.setOnClickListener(dialClickListener);
        btn5.setOnClickListener(dialClickListener);
        btn6.setOnClickListener(dialClickListener);
        btn7.setOnClickListener(dialClickListener);
        btn8.setOnClickListener(dialClickListener);
        btn9.setOnClickListener(dialClickListener);
        btn0.setOnClickListener(dialClickListener);
        btnStar.setOnClickListener(dialClickListener);

        // # toggles between text mode and 123 number mode
        btnHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitPendingChar();
                if (inputMode == 2) {
                    inputMode = 0;
                } else {
                    inputMode = 2;
                }
                updateKeyLabels();
            }
        });

        // Case toggle (abc -> ABC -> abc)
        btnCase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitPendingChar();
                if (inputMode == 0) {
                    inputMode = 1;
                } else {
                    inputMode = 0;
                }
                updateKeyLabels();
            }
        });

        // Backspace / Delete
        btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;
                if (hasPendingChar) {
                    handler.removeCallbacks(commitRunnable);
                    ic.finishComposingText();
                    ic.deleteSurroundingText(1, 0);
                    hasPendingChar = false;
                    lastKeyId = -1;
                    tapIndex = 0;
                } else {
                    ic.deleteSurroundingText(1, 0);
                }
            }
        });

        // Long press DEL clears text quickly
        btnDel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                commitPendingChar();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.deleteSurroundingText(50, 0);
                }
                return true;
            }
        });

        // Enter / Action
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitPendingChar();
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
                    int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                    if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                        ic.performEditorAction(action);
                        return;
                    }
                }
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            }
        });

        // '>' immediately commits current letter so you can type the next letter on the same button without waiting
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitPendingChar();
            }
        });

        updateKeyLabels();
        return root;
    }

    private void handleMultiTapKey(int keyId) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Number-only mode (123)
        if (inputMode == 2) {
            commitPendingChar();
            String num = getNumberForKey(keyId);
            if (num != null) {
                ic.commitText(num, 1);
            }
            return;
        }

        String chars = getCharsForKey(keyId);
        if (chars == null || chars.isEmpty()) return;

        // Space (0) commits immediately on first tap, or cycles 0 on second tap
        handler.removeCallbacks(commitRunnable);

        if (hasPendingChar && lastKeyId == keyId) {
            tapIndex = (tapIndex + 1) % chars.length();
        } else {
            commitPendingChar();
            lastKeyId = keyId;
            tapIndex = 0;
        }

        String currentChar = String.valueOf(chars.charAt(tapIndex));
        if (inputMode == 1) {
            currentChar = currentChar.toUpperCase();
        }

        ic.setComposingText(currentChar, 1);
        hasPendingChar = true;
        handler.postDelayed(commitRunnable, MULTI_TAP_TIMEOUT_MS);
    }

    private void commitPendingChar() {
        handler.removeCallbacks(commitRunnable);
        if (hasPendingChar) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
            hasPendingChar = false;
            lastKeyId = -1;
            tapIndex = 0;
        }
    }

    private String getCharsForKey(int keyId) {
        if (keyId == R.id.key_1) return ".,?!1'-()@/:_;";
        if (keyId == R.id.key_2) return "abc2";
        if (keyId == R.id.key_3) return "def3";
        if (keyId == R.id.key_4) return "ghi4";
        if (keyId == R.id.key_5) return "jkl5";
        if (keyId == R.id.key_6) return "mno6";
        if (keyId == R.id.key_7) return "pqrs7";
        if (keyId == R.id.key_8) return "tuv8";
        if (keyId == R.id.key_9) return "wxyz9";
        if (keyId == R.id.key_0) return " 0";
        if (keyId == R.id.key_star) return "*+-=@#$%&";
        return "";
    }

    private String getNumberForKey(int keyId) {
        if (keyId == R.id.key_1) return "1";
        if (keyId == R.id.key_2) return "2";
        if (keyId == R.id.key_3) return "3";
        if (keyId == R.id.key_4) return "4";
        if (keyId == R.id.key_5) return "5";
        if (keyId == R.id.key_6) return "6";
        if (keyId == R.id.key_7) return "7";
        if (keyId == R.id.key_8) return "8";
        if (keyId == R.id.key_9) return "9";
        if (keyId == R.id.key_0) return "0";
        if (keyId == R.id.key_star) return "*";
        return "";
    }

    private void updateKeyLabels() {
        if (inputMode == 2) {
            btnCase.setText("abc");
            btnHash.setText("#
ABC");
            btn1.setText("1");
            btn2.setText("2");
            btn3.setText("3");
            btn4.setText("4");
            btn5.setText("5");
            btn6.setText("6");
            btn7.setText("7");
            btn8.setText("8");
            btn9.setText("9");
            btn0.setText("0");
            btnStar.setText("*");
        } else if (inputMode == 1) {
            btnCase.setText("ABC");
            btnHash.setText("#
123");
            btn1.setText("1
.,?!");
            btn2.setText("2
ABC");
            btn3.setText("3
DEF");
            btn4.setText("4
GHI");
            btn5.setText("5
JKL");
            btn6.setText("6
MNO");
            btn7.setText("7
PQRS");
            btn8.setText("8
TUV");
            btn9.setText("9
WXYZ");
            btn0.setText("0
SPACE");
            btnStar.setText("*
+ - @");
        } else {
            btnCase.setText("abc");
            btnHash.setText("#
123");
            btn1.setText("1
.,?!");
            btn2.setText("2
abc");
            btn3.setText("3
def");
            btn4.setText("4
ghi");
            btn5.setText("5
jkl");
            btn6.setText("6
mno");
            btn7.setText("7
pqrs");
            btn8.setText("8
tuv");
            btn9.setText("9
wxyz");
            btn0.setText("0
SPACE");
            btnStar.setText("*
+ - @");
        }
    }

    @Override
    public void onFinishInput() {
        commitPendingChar();
        super.onFinishInput();
    }
}
