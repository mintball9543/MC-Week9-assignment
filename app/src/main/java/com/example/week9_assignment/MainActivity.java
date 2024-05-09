package com.example.week9_assignment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Hex;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    DatePicker dp;
    EditText edtDiary, edtPW, chkPW;
    Button btnWrite;
    String fileName;
    Switch swlock;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    boolean isLocked, flag = false;
    LayoutInflater inflater;
    private static final String privateKey_256 = "12345678900987654321123456789001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("간단 일기장");

        inflater = getLayoutInflater();

        sharedPreferences = this.getSharedPreferences("DiaryApp", MODE_PRIVATE);
        editor = sharedPreferences.edit();


        dp = (DatePicker) findViewById(R.id.datePicker);
        edtDiary = (EditText) findViewById(R.id.editText);
        btnWrite = (Button) findViewById(R.id.button);
        swlock = (Switch) findViewById(R.id.sw);

        boolean ispwd = sharedPreferences.getBoolean("ispwd",false);

        if(ispwd){
            swlock.setChecked(true);
            enablePW();
        }

        Calendar cal = Calendar.getInstance();
        int cYear = cal.get(Calendar.YEAR);
        int cMonth = cal.get(Calendar.MONTH);
        int cDay = cal.get(Calendar.DAY_OF_MONTH);

        dp.init(cYear, cMonth, cDay, new DatePicker.OnDateChangedListener() {
            public void onDateChanged(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                if(swlock.isChecked()){
                    enablePW();
                    return;
                }
                fileName = Integer.toString(year) + "_"
                        + Integer.toString(monthOfYear + 1) + "_"
                        + Integer.toString(dayOfMonth) + ".txt";
                String str = readDiary(fileName);

                edtDiary.setText(str);

            }
        });
        btnWrite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {

//                    if (mSwitch.isChecked()) {
//                        Toast.makeText(getApplicationContext(),
//                                "ReadOnly mode", Toast.LENGTH_SHORT).show();
//                        return;
//                    }


                    FileOutputStream outFs = openFileOutput(fileName, MODE_PRIVATE);
                    // FileOutputStream outFs = new FileOutputStream(new File(getFilesDir(), fileName));
                    String str = edtDiary.getText().toString();
                    outFs.write(str.getBytes());
                    outFs.close();
                    Toast.makeText(getApplicationContext(),
                            fileName + " 이 저장됨", Toast.LENGTH_SHORT).show();


                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),
                            "error:" + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        swlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(flag){
                    flag = false;
                    return;
                }

                if(swlock.isChecked()){
                    setPW();
                }else{
                    disablePW();
                }
            }
        });

    }

    //암호 설정 되어있을 시 일기장을 보여주지 않음
    private void enablePW(){
        edtDiary.setEnabled(false);
        edtDiary.setText("암호를 입력해 잠금을 해제하세요");
        edtDiary.setFreezesText(true);
        btnWrite.setEnabled(false);
        Toast.makeText(getApplicationContext(),
                "일기를 보려면 스위치를 다시 눌러 잠금을 해제하세요", Toast.LENGTH_SHORT).show();
    }

    //암호 설정
    private void setPW() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        dlg.setTitle("비밀번호 설정");
        View alertLayout = inflater.inflate(R.layout.alert, null);
        dlg.setView(alertLayout);

        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                edtPW = (EditText) alertLayout.findViewById(R.id.editTextPassword);
                String pwd = edtPW.getText().toString();
                try {
                    editor.putString("password", aesEncode(pwd));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                editor.putBoolean("ispwd", true);
                editor.apply();
                enablePW();
            }
        });

        dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                swlock.setChecked(false);
                flag = true;
            }
        });

        dlg.show();
    }

    //암호 해제
    private void disablePW() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        dlg.setTitle("비밀번호 해제");

        View alertLayout2 = inflater.inflate(R.layout.alert2, null);
        dlg.setView(alertLayout2);

        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String savedPassword = sharedPreferences.getString("password", "");
                try {
                    savedPassword = aesDecode(savedPassword);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                chkPW = (EditText) alertLayout2.findViewById(R.id.editTextPasswordCheck);
                String pwd = chkPW.getText().toString();

                if(savedPassword.equals(pwd)) {
                    edtDiary.setEnabled(true);
                    btnWrite.setEnabled(true);
                    edtDiary.setFreezesText(true);
                    edtDiary.setText("");
                    editor.putBoolean("ispwd", false);
                    editor.apply();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
                    flag = true;
                    swlock.setChecked(true);
                }
            }
        });

        dlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                flag = true;
                swlock.setChecked(true);
            }
        });

        dlg.show();
    }

    public static String aesEncode(String plainText) throws Exception {

        SecretKeySpec secretKey = new SecretKeySpec(privateKey_256.getBytes("UTF-8"), "AES");
        IvParameterSpec IV = new IvParameterSpec(privateKey_256.substring(0,16).getBytes());

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

        c.init(Cipher.ENCRYPT_MODE, secretKey, IV);

        byte[] encrpytionByte = c.doFinal(plainText.getBytes("UTF-8"));

        return Hex.encodeHexString(encrpytionByte);
    }


    public static String aesDecode(String encodeText) throws Exception {

        SecretKeySpec secretKey = new SecretKeySpec(privateKey_256.getBytes("UTF-8"), "AES");
        IvParameterSpec IV = new IvParameterSpec(privateKey_256.substring(0,16).getBytes());

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

        c.init(Cipher.DECRYPT_MODE, secretKey, IV);

        byte[] decodeByte = Hex.decodeHex(encodeText.toCharArray());

        return new String(c.doFinal(decodeByte), "UTF-8");
    }

    String readDiary(String fName) {
        String diaryStr = null;
        FileInputStream inFs;
        try {
            inFs = openFileInput(fName);
            // inFs=new  FileInputStream(new File(getFilesDir(), fileName));
            byte[] txt = new byte[500];
            inFs.read(txt);
            inFs.close();
            diaryStr = (new String(txt)).trim();
            btnWrite.setText("수정 하기");
        } catch (IOException e) {
            edtDiary.setHint("일기 없음");
            btnWrite.setText("새로 저장");
        }
        return diaryStr;
    }
}