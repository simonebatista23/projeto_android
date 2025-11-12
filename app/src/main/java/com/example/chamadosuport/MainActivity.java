package com.example.chamadosuport;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;
public class MainActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword;
    CheckBox chkRemember;
    Button btnLogin;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        chkRemember = findViewById(R.id.chkRemember);
        btnLogin = findViewById(R.id.btnLogin);

        db = new DatabaseHelper(this);


        checkSavedLogin();

        btnLogin.setOnClickListener(v -> {
            Log.d("LOGINTESTE", "Botão clicado!");
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                new LoginTask(email, password, chkRemember.isChecked()).execute();
            } else {
                showDialog("Erro", "Preencha todos os campos");
            }
        });


    }

    private void checkSavedLogin() {
        Cursor cursor = db.getUser();
        if (cursor.moveToFirst()) {
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            new LoginTask(email, password, true).execute();
        }
        cursor.close();
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show();
    }

    private class LoginTask extends AsyncTask<Void, Void, Boolean> {
        String email, password, token;
        boolean remember;
        String errorMessage;
        int userId;

        LoginTask(String email, String password, boolean remember) {
            this.email = email;
            this.password = password;
            this.remember = remember;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL url = new URL("http://192.168.0.116:5000/api/Users/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200 || responseCode == 201) ? conn.getInputStream() : conn.getErrorStream();

                byte[] buffer = new byte[1024];
                int bytesRead;
                StringBuilder sb = new StringBuilder();
                while ((bytesRead = is.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, bytesRead));
                }
                is.close();

                JSONObject resp = new JSONObject(sb.toString());

                if ((responseCode == 200 || responseCode == 201) && resp.has("token")) {
                    JSONObject tokenObject = resp.getJSONObject("token");
                    JSONObject userObject = resp.getJSONObject("user");

                    token = tokenObject.getString("token");
                    userId = userObject.getInt("id"); // 👈 pega o id do usuário

                    return true;
                } else {
                    errorMessage = resp.has("message") ? resp.getString("message") : "Erro desconhecido.";
                    return false;
                }

            } catch (Exception e) {
                errorMessage = "Falha: " + e.getMessage();
                return false;
            }
        }


        @Override
//
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (remember) db.saveUser(email, password);
                else db.clearUser();
                Intent intent = new Intent(MainActivity.this, NewTicketActivity.class);
                intent.putExtra("token", token);
                intent.putExtra("userId", userId); // 👈 envia o id para a segunda tela
                startActivity(intent);

//                startActivity(new Intent(MainActivity.this, UsersActivity.class));
            } else {
                showDialog("Erro", errorMessage);
            }
        }
    }
}
