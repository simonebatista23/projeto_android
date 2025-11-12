package com.example.chamadosuport;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class NewTicketActivity extends AppCompatActivity {

    int userId;
    String token;
    LinearLayout containerTickets;
    Button btnNovo, btnSair;

    private Handler handler = new Handler(); //  Handler para atualização automática
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_ticket);

        containerTickets = findViewById(R.id.containerTickets);
        btnNovo = findViewById(R.id.btnNovo);
        btnSair = findViewById(R.id.btnSair);

        token = getIntent().getStringExtra("token");
        userId = getIntent().getIntExtra("userId", -1);

        new LoadTicketsTask().execute();

        // Atualiza a lista automaticamente a cada 1 segundo
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                new LoadTicketsTask().execute();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateRunnable, 1000);

        btnNovo.setOnClickListener(v -> {
            Intent i = new Intent(NewTicketActivity.this, CreateTicketActivity.class);
            i.putExtra("token", token);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        btnSair.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable); //  para o loop ao sair da tela
    }

    private class LoadTicketsTask extends AsyncTask<Void, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(Void... voids) {
            try {
                URL url = new URL("http://192.168.0.116:5000/api/Ticket/opened/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.connect();

                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                return new JSONArray(response);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray tickets) {
            if (tickets != null) {
                containerTickets.removeAllViews(); // limpa antes de recarregar

                for (int i = 0; i < tickets.length(); i++) {
                    try {
                        JSONObject t = tickets.getJSONObject(i);

                        View card = getLayoutInflater().inflate(R.layout.item_ticket, null);
                        TextView txtDesc = card.findViewById(R.id.txtDesc);
                        TextView txtStatus = card.findViewById(R.id.txtStatus);

                        int statusId = t.getInt("id_status");
                        String statusText;

                        if (statusId == 1) {
                            statusText = "Aberto";
                        } else if (statusId == 2) {
                            statusText = "Em Andamento";
                        } else if (statusId == 3) {
                            statusText = "Concluído";
                        } else {
                            statusText = "Outro Status (" + statusId + ")";
                        }

                        txtDesc.setText("Ticket #" + t.getInt("id") + ": " + t.getString("desc"));
                        txtStatus.setText("Status: " + statusText);

                        containerTickets.addView(card);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
