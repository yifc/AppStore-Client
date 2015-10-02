package com.y3.appstoreclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yifei.appstoreclient.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class UserActivity extends Activity {

    private TextView userName;
    private TextView appsBy;
    private TextView friendList;
    private ListView listView;
    private ProgressDialog mProgressDialog;
    private ArrayList<myFile> fileList;
    private ArrayAdapter<String> adapter;
    private List<String> fileInfo = new ArrayList<String>();
    public static final String GET_URL = "http://10.1.11.33/y3/elgg-1.8.20/services/api/rest/xml/";

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        adapter = new ArrayAdapter<String>(this,R.layout.list_view_row_item, R.id.textViewItem, fileInfo);
        initView();
    }

    private boolean initView(){

        mProgressDialog = new ProgressDialog(UserActivity.this);
        mProgressDialog.setMessage("Download");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        Intent receivedIntent = getIntent();
        String username = receivedIntent.getStringExtra("username");
        userName = (TextView)findViewById(R.id.text_name);
        userName.setText(username);
        appsBy = (TextView)findViewById(R.id.appsBy);
        appsBy.setText("Recent Applications by all developers\nTouch to download");
        String loginUrl = GET_URL + "?method=listfiles&context=all&username=" + username;
        try{
            new MyAsyncTask_ListFiles().execute(loginUrl);
        }
        catch (Exception e){
            return false;
        }
        return true;
    }


    class MyAsyncTask_ListFiles extends AsyncTask<String, Integer, ArrayList<myFile>> {

        URL UrlObject = null;
        String status = null;

        // <summary>
        // In this function we will call the restful API remotely to obtain the file list
        // </summary>
        // <param name="strings[0]">The http url refer the restful API which include method and parameter</param>
        // <returns>ArrayList which contain the file information</returns>
        @Override
        protected ArrayList<myFile> doInBackground(String... strings) {
            try {
                UrlObject = new URL(strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection connection = null;
            fileList = new ArrayList<myFile>();
            try {
                connection = (HttpURLConnection) UrlObject.openConnection();
                InputStream in = new BufferedInputStream(connection.getInputStream());

                // Parse the XML Result
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(in);
                //doc.getDocumentElement().normalize();

                System.out.println("--------------------------------------------------------");
                System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
                System.out.println("--------------------------------------------------------");
                Element eElement = doc.getDocumentElement();
                status = eElement.getElementsByTagName("status").item(0).getTextContent();
                //result = eElement.getElementsByTagName("result").item(0).getTextContent();
                System.out.println("status : " + status);
                if(status.equals("0"))
                {
                    // Parse the xml result and add file information to the ArrayList
                    NodeList list = doc.getElementsByTagName("array_item");
                    for(int index = 0; index <list.getLength(); index++){
                        Element element = (Element)list.item(index);
                        String guid = element.getElementsByTagName("guid").item(0).getTextContent();
                        String title = element.getElementsByTagName("title").item(0).getTextContent();
                        String owner_name = element.getElementsByTagName("owner_name").item(0).getTextContent();
                        myFile file = new myFile();
                        file.setGuid(guid);
                        file.setTitle(title);
                        file.setOwner_name(owner_name);
                        fileList.add(file);
                    }
                }
                //System.out.println("result : " + result);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return fileList;
        }

        // <summary>
        // This function run automatically after doInBackground function, display the file information at the list view
        // </summary>
        // <param name="result">Thr parameter is passed from the return value of doInBackground function which contains all the file information</param>
        // <returns>void</returns>
        @Override
        protected void onPostExecute(ArrayList<myFile> result){
            for(int index = 0; index < result.size(); index++){
                myFile tempFile = result.get(index);
                String tempString = tempFile.getTitle() + " by " + tempFile.getOwner_name();
                adapter.add(tempString);
                //fileInfo.add(tempString);
            }
            listView = (ListView) findViewById(R.id.mainListView);
            listView.setAdapter(adapter);
            //Set OnItemClickListener, when we click on the any item of list, the corresponding file will be downloaded.
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String download_url = "http://10.1.11.33/y3/elgg-1.8.20/file/download/" + fileList.get(i).getGuid();
                    new MyAsyncTask_DownloadFiles(UserActivity.this).execute(download_url, fileList.get(i).getTitle());
                }
            });
            /*listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                //Long Click to add a the developer to be friends
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String addFriend_url = ""
                }
            });*/
        }
    }

    class MyAsyncTask_DownloadFiles extends AsyncTask<String, Integer, String>{

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private String filePath = "";

        public MyAsyncTask_DownloadFiles(Context context){
            this.context = context;
        }

        // <summary>
        // This function will download the file selected in a new thread.
        // </summary>
        // <param name="strings[0]">The http url refer the restful API which include method and parameter</param>
        // <returns>ArrayList which contain the file information</returns>
        @Override
        protected String doInBackground(String... strings) {
            // TODO Auto-generated method stub
            InputStream in = null;
            OutputStream out = null;
            HttpURLConnection connection = null;
            File downloadFile;
            try{
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.connect();
                //int fileSize = connection.getContentLength();
                int fileSize = 3743416;
                in = connection.getInputStream();
                filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + strings[1] + ".apk";
                downloadFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + strings[1] + ".apk");
                out = new FileOutputStream(downloadFile);


                byte data[] = new byte[100];
                long currentSize = 0;
                int count;
                while ((count = in.read(data)) != -1){
                    if(isCancelled()){
                        in.close();
                        return null;
                    }
                    currentSize += count;
                    if (fileSize > 0)
                        publishProgress((int)(currentSize * 100 / fileSize));
                    out.write(data, 0, count);
                }
            } catch(Exception e){
                return e.toString();
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                } catch (IOException ignored){
                }

                if (connection != null)
                    connection.disconnect();
            }

            return null;
        }
        // <summary>
        // Before doInBackground function we will set Wake Lock and display the progress dialog
        // </summary>
        // <param></param>
        // <returns></returns>
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(Integer... percent){
            super.onProgressUpdate(percent);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(percent[0]);
        }
        @Override
        protected void onPostExecute(String result){
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: "+result, Toast.LENGTH_LONG).show();
            else
            {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                Intent install = new Intent(Intent.ACTION_VIEW);
                File file = new File(filePath);
                install.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(install);
            }
        }
    }
//
//    class MyAsyncTask_ListFriends extends AsyncTask<String, Integer, String>{
//
//        HttpURLConnection connection = null;
//        @Override
//        protected String doInBackground(String... strings) {
//            URL url = null;
//            try {
//                url = new URL(strings[0]);
//                connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestProperty("Accept-Encoding", "identity");
//                connection.connect();
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }

    // Object of this class contain the information of file
    class myFile{
        private String guid;
        private String title;
        private String owner_name;


        public void setGuid(String guid){
            this.guid = guid;
        }
        public String getGuid(){
            return this.guid;
        }

        public void setTitle(String title){
            this.title = title;
        }
        public String getTitle(){
            return this.title;
        }

        public void setOwner_name(String owner_name){
            this.owner_name = owner_name;
        }
        public String getOwner_name() { return this.owner_name; }
    }

    class MyAsyncTask_logout extends AsyncTask<String, Integer, String>{
        private Context context;
        public MyAsyncTask_logout(Context context){
            this.context = context;
        }
        protected String doInBackground(String... strings) {
            URL UrlObject = null;
            String status = null;
            String result = null;
            try {
                UrlObject = new URL(strings[0]);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) UrlObject.openConnection();
                InputStream in = new BufferedInputStream(connection.getInputStream());

                // Parse the XML Result
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(in);
                doc.getDocumentElement().normalize();

                System.out.println("--------------------------------------------------------");
                System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
                System.out.println("--------------------------------------------------------");
                Element eElement = doc.getDocumentElement();
                status = eElement.getElementsByTagName("status").item(0).getTextContent();
                if (status.equals("-1")) {
                    result = eElement.getElementsByTagName("message").item(0).getTextContent();
                }
                if (status.equals("0")) {
                    result = eElement.getElementsByTagName("result").item(0).getTextContent();
                }
                System.out.println("status : " + status);
                System.out.println("result : " + result);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return result;
        }
        protected void onPostExecute(String result){
            if(result.equals("true")){
                Intent intent = new Intent();
                intent.setClass(UserActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else{
                CharSequence text = "We could not log you out. Please try again later.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                toast.show();
            }
        }
    }

    public boolean logout() {
        String logoutUrl = GET_URL + "?method=logout";
        try {
            new MyAsyncTask_logout(UserActivity.this).execute(logoutUrl);
        } catch (Exception e) {
            return false;
        }
        return true;
    }



    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();
        if (id == R.id.action_logout) {
            return true;
        }
        return super.onOptionsItemSelected(item);*/
        switch(item.getItemId()){
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_apps_all:
                //String appsAllUrl = GET_URL + "?methd=listfiles";
                Intent intent1 = new Intent();
                intent1.setClass(UserActivity.this, UserActivity.class);
                intent1.putExtra("username", userName.getText().toString());
                startActivity(intent1);
                finish();
                return true;
            case R.id.action_apps_friends:
                //String appsFriendsUrl = GET_URL + "?method=listfiles";
                Intent intent2 = new Intent(UserActivity.this, AppsByFriendsActivity.class);
                intent2.putExtra("username", userName.getText().toString());
                startActivity(intent2);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}