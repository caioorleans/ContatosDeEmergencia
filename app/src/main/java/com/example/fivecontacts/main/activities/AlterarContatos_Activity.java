package com.example.fivecontacts.main.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.fivecontacts.R;
import com.example.fivecontacts.main.model.Contato;
import com.example.fivecontacts.main.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AlterarContatos_Activity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    Boolean primeiraVezUser=true;
    EditText edtNome;
    ListView lv;
    BottomNavigationView bnv;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_contatos);
        edtNome = findViewById(R.id.edtBusca);
        bnv = findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(this);
        bnv.setSelectedItemId(R.id.anvMudar);

        //Dados da Intent Anterior
        Intent quemChamou=this.getIntent();
        if (quemChamou!=null) {
            Bundle params = quemChamou.getExtras();
            if (params!=null) {
                //Recuperando o Usuario
                user = (User) params.getSerializable("usuario");
                setTitle("Alterar Contatos de Emerg??ncia");
            }
        }
        lv = findViewById(R.id.listContatosDoCell);
        //Evento de limpar Componente
        edtNome.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (primeiraVezUser){
                    primeiraVezUser=false;
                    edtNome.setText("");
                }

                return false;
            }
        });
    }

    public void salvarContato (Contato w){
        if(!estaNaLista(w)){
            SharedPreferences salvaQuantidade= getSharedPreferences("quantidadeContatos", Activity.MODE_PRIVATE);
            if(salvaQuantidade.getInt("quantidade", 0) < 5){
                SharedPreferences salvaContatos =
                        getSharedPreferences("contatos",Activity.MODE_PRIVATE);

                int num = salvaContatos.getInt("numContatos", 0); //checando quantos contatos j?? tem
                SharedPreferences.Editor editor = salvaContatos.edit();
                try {
                    ByteArrayOutputStream dt = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(dt);
                    dt = new ByteArrayOutputStream();
                    oos = new ObjectOutputStream(dt);
                    oos.writeObject(w);
                    String contatoSerializado= dt.toString(StandardCharsets.ISO_8859_1.name());
                    editor.putString("contato"+(num+1), contatoSerializado);
                    editor.putInt("numContatos",num+1);
                }catch(Exception e){
                    e.printStackTrace();
                }
                editor.commit();
                user.getContatos().add(w);
                SharedPreferences.Editor escritor3= salvaQuantidade.edit();

                escritor3.putInt("quantidade",salvaQuantidade.getInt("quantidade",0)+1);

                escritor3.commit();
            }
            else{
                Toast.makeText(this, "Permitido salvar no m??ximo 5 contatos", Toast.LENGTH_LONG).show();
            }

        }
        else{
            Toast.makeText(this, "Contato j?? est?? na lista!", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.v("PDM","Matando a Activity Lista de Contatos");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("PDM","Matei a Activity Lista de Contatos");
    }


    public void onClickBuscar(View v){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            Log.v("PDM", "Pedir permiss??o");
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 3333);
            return;
        }
        Log.v("PDM", "Tenho permiss??o");

        ContentResolver cr = getContentResolver();
        String consulta = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
        String [] argumentosConsulta= {"%"+edtNome.getText()+"%"};
        Cursor cursor= cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                consulta,argumentosConsulta, null);
        final String[] nomesContatos = new String[cursor.getCount()];
        final String[] telefonesContatos = new String[cursor.getCount()];
        Log.v("PDM","Tamanho do cursor:"+cursor.getCount());

        int i=0;
        while (cursor.moveToNext()) {
            int indiceNome = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            String contatoNome = cursor.getString(indiceNome);
            Log.v("PDM", "Contato " + i + ", Nome:" + contatoNome);
            nomesContatos[i]= contatoNome;
            int indiceContatoID = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            String contactID = cursor.getString(indiceContatoID);
            String consultaPhone = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactID;
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, consultaPhone, null, null);

            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                telefonesContatos[i]=number; //Salvando s?? ??ltimo telefone
            }
            i++;
        }

        if (nomesContatos !=null) {
            ArrayList<Map<String,Object>> itemDataList = new ArrayList<Map<String,Object>>();;

            for(i =0; i < nomesContatos.length; i++) {
                Map<String,Object> listItemMap = new HashMap<String,Object>();
                listItemMap.put("imageId", R.drawable.ic_action_mudar);
                listItemMap.put("contato", nomesContatos[i]);
                itemDataList.add(listItemMap);
            }
            SimpleAdapter simpleAdapter = new SimpleAdapter(this,itemDataList,R.layout.list_view_layout2,
                    new String[]{"imageId","contato"},new int[]{R.id.imagemLigar, R.id.nomeContato});

            lv.setAdapter(simpleAdapter);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Contato c= new Contato();
                    c.setNome(nomesContatos[i]);
                    c.setNumero("tel:+"+telefonesContatos[i]);
                    salvarContato(c);
                    Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                    intent.putExtra("usuario", user);
                    startActivity(intent);
                    finish();

                }
            });
            /*for(int j=0; j<=nomesContatos.length; j++) {
                ArrayAdapter<String> adaptador;
                adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesContatos);

                lv.setAdapter(adaptador);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Contato c= new Contato();
                        c.setNome(nomesContatos[i]);
                        c.setNumero("tel:+"+telefonesContatos[i]);
                        salvarContato(c);
                        Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                        intent.putExtra("usuario", user);
                        startActivity(intent);
                        finish();

                    }
                });
            }*/
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Checagem de o Item selecionado ?? o do perfil
        if (item.getItemId() == R.id.anvPerfil) {
            //Abertura da Tela MudarDadosUsario
            Intent intent = new Intent(this, PerfilUsuario_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        // Checagem de o Item selecionado ?? o do perfil
        if (item.getItemId() == R.id.anvLigar) {
            //Abertura da Tela Mudar COntatos
            Intent intent = new Intent(this, ListaDeContatos_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        return true;
    }

   private boolean estaNaLista(Contato w){
        boolean estaNaLista = false;
       SharedPreferences recuperarContatos = getSharedPreferences("contatos", Activity.MODE_PRIVATE);

       int num = recuperarContatos.getInt("numContatos", 0);
       ArrayList<Contato> contatos = new ArrayList<Contato>();

       Contato contato;


       for (int i = 1; i <= num; i++) {
           String objSel = recuperarContatos.getString("contato" + i, "");
           if (objSel.compareTo("") != 0) {
               try {
                   ByteArrayInputStream bis =
                           new ByteArrayInputStream(objSel.getBytes(StandardCharsets.ISO_8859_1.name()));
                   ObjectInputStream oos = new ObjectInputStream(bis);
                   contato = (Contato) oos.readObject();

                   if (contato.getNumero().equals(w.getNumero())  && contato.getNome().equals(w.getNome())) {
                       estaNaLista = true;
                   }

               } catch (Exception e) {
                   e.printStackTrace();
               }

           }
       }
       return estaNaLista;
   }
}