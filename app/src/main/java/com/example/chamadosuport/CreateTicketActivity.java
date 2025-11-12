package com.example.chamadosuport;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.MediaType;
import okhttp3.Response;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText edtDesc, edtBody;
    private Spinner spnDept;
    private Button btnSelectFile, btnCreateTicket;
    private TextView txtSelectedFile;

    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private String token = "";

    private static final int PICK_FILE_REQUEST = 1;
    private static final int REQUEST_PERMISSION = 200;
    private ArrayList<String> deptNames = new ArrayList<>();
    private ArrayList<Integer> deptIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);

        edtDesc = findViewById(R.id.edtDesc);
        edtBody = findViewById(R.id.edtBody);
        spnDept = findViewById(R.id.spnDept);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnCreateTicket = findViewById(R.id.btnCreateTicket);
        txtSelectedFile = findViewById(R.id.txtSelectedFile);

        token = getIntent().getStringExtra("token");

        new LoadDepartmentsTask().execute();

        btnSelectFile.setOnClickListener(v -> requestFilePermission());
        btnCreateTicket.setOnClickListener(v -> enviarTicket());
    }

    // =================== CARREGAR DEPARTAMENTOS ===================
    private class LoadDepartmentsTask extends AsyncTask<Void, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL url = new URL("http://192.168.0.116:5000/api/Depts/ativos");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    errorMessage = "Erro HTTP: " + responseCode;
                    return false;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(sb.toString());

                deptNames.clear();
                deptIds.clear();
                deptNames.add("-- Selecione o Departamento --");
                deptIds.add(0);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    deptIds.add(id);
                    deptNames.add(name);
                }
                return true;

            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateTicketActivity.this, android.R.layout.simple_spinner_item, deptNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnDept.setAdapter(adapter);
            } else {
                Toast.makeText(CreateTicketActivity.this, "Erro ao carregar departamentos: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    // =================== PERMISSÃO E SELEÇÃO DE ARQUIVO ===================
    private void requestFilePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            selectFile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            try (Cursor cursor = getContentResolver().query(selectedFileUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    selectedFileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
            txtSelectedFile.setText("Selecionado: " + selectedFileName);
        }
    }

    // =================== ENVIO DO TICKET ===================
    private void enviarTicket() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Selecione um arquivo", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = edtDesc.getText().toString().trim();
        String body = edtBody.getText().toString().trim();
        int deptPosition = spnDept.getSelectedItemPosition();
        int deptId = deptIds.get(deptPosition);

        if (deptId == 0) {
            Toast.makeText(this, "Selecione um departamento", Toast.LENGTH_SHORT).show();
            return;
        }
        if (desc.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        new CreateTicketTask(desc, body, deptId).execute();
    }

    public File getFileFromUri(Context context, Uri uri) {
        String fileName = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (index >= 0) fileName = cursor.getString(index);
            cursor.close();
        }

        if (fileName == null) fileName = "temp_file";

        File file = new File(context.getCacheDir(), fileName);
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(file)) {

            if (input == null) return null;

            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                byte[] decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
                String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
                JSONObject obj = new JSONObject(decoded);
                if (obj.has("idUser")) {
                    return obj.getInt("idUser");
                }
            }
        } catch (Exception e) {
            Log.e("JWT_ERROR", "Falha ao extrair ID do token.", e);
        }
        return 1;
    }

    private class CreateTicketTask extends AsyncTask<Void, Void, Boolean> {

        private String desc, body;
        private int deptId;
        private String errorMessage = "";

        CreateTicketTask(String desc, String body, int deptId) {
            this.desc = desc;
            this.body = body;
            this.deptId = deptId;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                OkHttpClient client = new OkHttpClient();

                File file = getFileFromUri(CreateTicketActivity.this, selectedFileUri);
                if (file == null || !file.exists()) {
                    errorMessage = "Não foi possível abrir o arquivo.";
                    return false;
                }

                RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
                int userId = getUserIdFromToken(token);

                MultipartBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("Id_user", String.valueOf(userId))
                        .addFormDataPart("Id_dept_target", String.valueOf(deptId))
                        .addFormDataPart("Id_status", "1")
                        .addFormDataPart("Desc", desc)
                        .addFormDataPart("Body", body)
                        .addFormDataPart("File", file.getName(), fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url("http://192.168.0.116:5000/api/Ticket/create-with-transaction")
                        .addHeader("Authorization", "Bearer " + token)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String respBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    Log.d("CREATE_TICKET", "Sucesso: " + respBody);
                    return true;
                } else {
                    errorMessage = respBody;
                    Log.e("CREATE_TICKET", "Falha: " + respBody);
                    return false;
                }

            } catch (Exception e) {
                errorMessage = e.getMessage();
                Log.e("CREATE_TICKET", "Erro: ", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(CreateTicketActivity.this, "Ticket criado com sucesso!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(CreateTicketActivity.this, "Falha: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}
