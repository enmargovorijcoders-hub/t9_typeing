package com.enmar.t9;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

public class T9InputMethodService extends InputMethodService {

    // Case modes: 0 = abc, 1 = Abc, 2 = ABC
    private static final int CASE_LOWER = 0;
    private static final int CASE_CAP_NEXT = 1;
    private static final int CASE_ALL_CAPS = 2;

    private static final long TAP_TIMEOUT_MS = 850L;

    private static final String[] KEY_1_CHARS = {".", ",", "?", "!", "+", "-", "@", "/", "_", "1"};
    private static final String[] KEY_2_CHARS = {"a", "b", "c", "2"};
    private static final String[] KEY_3_CHARS = {"d", "e", "f", "3"};
    private static final String[] KEY_4_CHARS = {"g", "h", "i", "4"};
    private static final String[] KEY_5_CHARS = {"j", "k", "l", "5"};
    private static final String[] KEY_6_CHARS = {"m", "n", "o", "6"};
    private static final String[] KEY_7_CHARS = {"p", "q", "r", "s", "7"};
    private static final String[] KEY_8_CHARS = {"t", "u", "v", "8"};
    private static final String[] KEY_9_CHARS = {"w", "x", "y", "z", "9"};
    private static final String[] KEY_0_CHARS = {" ", "+", "0"};
    private static final String[] KEY_STAR_CHARS = {"*"};

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView statusBar;
    private Button keyCase;
    private Button keyHash;

    private int caseMode = CASE_CAP_NEXT; // Default to Abc
    private boolean numberMode = false;

    private int lastKeyId = -1;
    private int tapIndex = 0;
    private boolean hasPendingChar = false;

    private final Runnable commitRunnable = this::commitPendingTap;

    @Override
    public View onCreateInputView() {
        View root = getLayoutInflater().inflate(R.layout.keyboard_view, null);

        statusBar = root.findViewById(R.id.status_bar);
        keyCase = root.findViewById(R.id.key_case);
        keyHash = root.findViewById(R.id.key_hash);

        bindMultiTapKey(root.findViewById(R.id.key_1), R.id.key_1, KEY_1_CHARS, "1");
        bindMultiTapKey(root.findViewById(R.id.key_2), R.id.key_2, KEY_2_CHARS, "2");
        bindMultiTapKey(root.findViewById(R.id.key_3), R.id.key_3, KEY_3_CHARS, "3");
        bindMultiTapKey(root.findViewById(R.id.key_4), R.id.key_4, KEY_4_CHARS, "4");
        bindMultiTapKey(root.findViewById(R.id.key_5), R.id.key_5, KEY_5_CHARS, "5");
        bindMultiTapKey(root.findViewById(R.id.key_6), R.id.key_6, KEY_6_CHARS, "6");
        bindMultiTapKey(root.findViewById(R.id.key_7), R.id.key_7, KEY_7_CHARS, "7");
        bindMultiTapKey(root.findViewById(R.id.key_8), R.id.key_8, KEY_8_CHARS, "8");
        bindMultiTapKey(root.findViewById(R.id.key_9), R.id.key_9, KEY_9_CHARS, "9");
        bindMultiTapKey(root.findViewById(R.id.key_0), R.id.key_0, KEY_0_CHARS, "0");
        bindMultiTapKey(root.findViewById(R.id.key_star), R.id.key_star, KEY_STAR_CHARS, "*");

        Button keyDel = root.findViewById(R.id.key_del);
        keyDel.setOnClickListener(v -> handleDelete());
        keyDel.setOnLongClickListener(v -> {
            commitPendingTap();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.deleteSurroundingText(1000, 1000);
            }
            updateStatus("cleared");
            return true;
        });

        keyCase.setOnClickListener(v -> {
            commitPendingTap();
            numberMode = false;
            caseMode = (caseMode + 1) % 3;
            updateModeLabels();
            updateStatus("mode switched");
        });

        keyHash.setOnClickListener(v -> {
            commitPendingTap();
            numberMode = !numberMode;
            if (!numberMode && caseMode == CASE_LOWER) {
                caseMode = CASE_CAP_NEXT;
            }
            updateModeLabels();
            updateStatus(numberMode ? "numbers" : "text");
        });

        Button keyEnter = root.findViewById(R.id.key_enter);
        keyEnter.setOnClickListener(v -> handleEnter());

        Button keyNext = root.findViewById(R.id.key_next);
        keyNext.setOnClickListener(v -> {
            commitPendingTap();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
            }
            updateStatus("next");
        });

        updateModeLabels();
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        resetTapState();
        applyInputType(info);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        resetTapState();
        applyInputType(attribute);
    }

    private void applyInputType(EditorInfo info) {
        int inputClass = info != null ? (info.inputType & InputType.TYPE_MASK_CLASS) : InputType.TYPE_CLASS_TEXT;
        int variation = info != null ? (info.inputType & InputType.TYPE_MASK_VARIATION) : 0;

        boolean requiresNumber = (inputClass == InputType.TYPE_CLASS_NUMBER)
                || (inputClass == InputType.TYPE_CLASS_PHONE)
                || (inputClass == InputType.TYPE_CLASS_DATETIME)
                || (inputClass == InputType.TYPE_CLASS_TEXT
                && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD));

        if (requiresNumber) {
            numberMode = true;
            updateStatus("auto 123");
        } else {
            numberMode = false;
            caseMode = CASE_CAP_NEXT; // Default to Abc for text fields
            updateStatus("auto Abc");
        }
        updateModeLabels();
    }

    private void bindMultiTapKey(Button button, int keyId, String[] chars, String digitChar) {
        button.setOnClickListener(v -> {
            if (numberMode) {
                commitPendingTap();
                commitText(digitChar, false);
                return;
            }
            handleMultiTap(keyId, chars);
        });

        button.setOnLongClickListener(v -> {
            cancelPendingTimer();
            removePendingChar();
            resetTapState();
            commitText(digitChar, false);
            updateStatus("inserted " + digitChar);
            return true;
        });
    }

    private void handleMultiTap(int keyId, String[] chars) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        if (hasPendingChar && lastKeyId == keyId) {
            removePendingChar();
            tapIndex = (tapIndex + 1) % chars.length;
        } else {
            commitPendingTap();
            lastKeyId = keyId;
            tapIndex = 0;
        }

        String raw = chars[tapIndex];
        boolean wasCapitalized = false;
        String output = applyCase(raw);
        if (!raw.equals(output) && raw.length() == 1 && Character.isLetter(raw.charAt(0))) {
            wasCapitalized = true;
        }

        ic.commitText(output, 1);
        hasPendingChar = true;

        if (wasCapitalized && caseMode == CASE_CAP_NEXT) {
            caseMode = CASE_LOWER;
            updateModeLabels();
        }

        updateStatus("key " + output);
        scheduleCommitTimer();
    }

    private String applyCase(String s) {
        if (s == null || s.isEmpty() || s.length() > 1) return s;
        char c = s.charAt(0);
        if (!Character.isLetter(c)) return s;

        if (caseMode == CASE_ALL_CAPS) {
            return String.valueOf(Character.toUpperCase(c));
        } else if (caseMode == CASE_CAP_NEXT) {
            return String.valueOf(Character.toUpperCase(c));
        } else {
            return String.valueOf(Character.toLowerCase(c));
        }
    }

    private void removePendingChar() {
        if (!hasPendingChar) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(1, 0);
        }
        hasPendingChar = false;
    }

    private void commitPendingTap() {
        cancelPendingTimer();
        hasPendingChar = false;
        lastKeyId = -1;
        tapIndex = 0;
    }

    private void scheduleCommitTimer() {
        cancelPendingTimer();
        handler.postDelayed(commitRunnable, TAP_TIMEOUT_MS);
    }

    private void cancelPendingTimer() {
        handler.removeCallbacks(commitRunnable);
    }

    private void resetTapState() {
        cancelPendingTimer();
        hasPendingChar = false;
        lastKeyId = -1;
        tapIndex = 0;
    }

    private void commitText(String text, boolean resetAfterSpace) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            if (resetAfterSpace && " ".equals(text) && !numberMode && caseMode == CASE_LOWER) {
                caseMode = CASE_CAP_NEXT;
                updateModeLabels();
            }
        }
    }

    private void handleDelete() {
        if (hasPendingChar) {
            removePendingChar();
            resetTapState();
            updateStatus("undid tap");
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
        updateStatus("deleted");
    }

    private void handleEnter() {
        commitPendingTap();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && (ei.imeOptions & EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(ei.imeOptions & EditorInfo.IME_MASK_ACTION);
        } else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
        if (!numberMode) {
            caseMode = CASE_CAP_NEXT;
            updateModeLabels();
        }
        updateStatus("enter");
    }

    private void updateModeLabels() {
        if (keyCase != null) {
            if (numberMode) {
                keyCase.setText("Abc");
            } else if (caseMode == CASE_ALL_CAPS) {
                keyCase.setText("ABC");
            } else if (caseMode == CASE_CAP_NEXT) {
                keyCase.setText("Abc");
            } else {
                keyCase.setText("abc");
            }
        }
        if (keyHash != null) {
            keyHash.setText(numberMode ? "#\nabc" : "#\n123");
        }
    }

    private void updateStatus(String detail) {
        if (statusBar == null) return;
        String modeLabel = numberMode ? "123" : (caseMode == CASE_ALL_CAPS ? "ABC" : (caseMode == CASE_CAP_NEXT ? "Abc" : "abc"));
        statusBar.setText(modeLabel + "  •  " + detail);
    }
}
