package com.example.yakitori;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MyActivity extends Activity implements View.OnClickListener, SensorEventListener
{
    TextView textView;
    TextView totalcalTextView;
    TextView caloriev;
    //  Button button;

    private SensorManager manager;
    private TextView calView, detailView;
    int cal = 0;
    int total_cal;
    int remaining_cal;

    String food_id = "";

    @Override
    protected void onStop() {
        super.onStop();
        manager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() > 0) {
            Sensor s = sensors.get(0);
            manager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long timestamp = System.currentTimeMillis();

            float g = Math.abs((float) (Math.sqrt(
                    event.values[SensorManager.DATA_X] * event.values[SensorManager.DATA_X]
                            + event.values[SensorManager.DATA_Y] * event.values[SensorManager.DATA_Y]
                            + event.values[SensorManager.DATA_Z] * event.values[SensorManager.DATA_Z]) - 9.6)) / 10;
            cal += g;
            total_cal += g;

            String str = "加速度センサー値:"
                    + "\nX軸:" + event.values[SensorManager.DATA_X]
                    + "\nY軸:" + event.values[SensorManager.DATA_Y]
                    + "\nZ軸:" + event.values[SensorManager.DATA_Z]
                    + "\nG:" + g;

            if (cal > 9) {
                PostMessageTask post = new PostMessageTask(cal);
                post.execute("");
                cal = 0;

                //サジェストメニューを表示
                GetMessageTask get = new GetMessageTask();
                get.execute("");
            }
        }
        totalcalTextView.setText(total_cal + "");

        if(remaining_cal < total_cal){
            Log.w("OK", "OK");
            remaining_cal = remaining_cal + 1000;
            showAlert();
            caloriev.setText("目標達成済！");
        }

       // caloriev.setText(remaining_cal+" Kcal");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // text_view1： activity_main.xml の TextView の
        setContentView(R.layout.main);

        totalcalTextView = (TextView) findViewById(R.id.total_calorie);
        //   button = (Button) findViewById(R.id.button);
        //   button.setOnClickListener((View.OnClickListener) this);

        GetMessageTask get = new GetMessageTask();
        get.execute("");
        GetMessageTaskWeather getWeather = new GetMessageTaskWeather();
        getWeather.execute("");
        GetMessageTaskUser getUser = new GetMessageTaskUser();
        getUser.execute("");

    }

    @Override
    public void onClick(View v) {
        /*
        try {
            HttpClient httpClient = new DefaultHttpClient();
            // PHPサーバ
            Log.w("HTTP", "1");
            HttpGet httpGet = new HttpGet("http://www.widret.sakura.ne.jp/cands/index.php/api/v1/users/id/4/");
            Log.w("HTTP", "2");
            HttpResponse httpResponse = httpClient.execute(httpGet);
            Log.w("HTTP", "3");
            String str = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

            textView.setText(str);
            Log.w("HTTP", str);
        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
        */
        //PostMessageTask post = new PostMessageTask();
        //post.execute("");
    }

    public class PostMessageTask extends AsyncTask<String, Integer, String> {

        int sendCal;
        public PostMessageTask(int c){
            sendCal = c;
        }

        @Override
        protected String doInBackground(String... contents) {
            String url="index.php/api/v1/users/id/0/";
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);

            ArrayList<NameValuePair> params = new ArrayList <NameValuePair>();
            params.add( new BasicNameValuePair("id", "0"));
            params.add( new BasicNameValuePair("calorie", ""+ sendCal));

            HttpResponse res = null;
            String str = "";

            try {
                post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
                res = httpClient.execute(post);
                str = EntityUtils.toString(res.getEntity(), "UTF-8");
                Log.w("HTTP", str);;
                Log.w("HTTP2", str);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return str;
        }

        @Override
        protected void onPostExecute(String str) {
            //totalcalTextView.setText(total_cal);
        }
    }

    //料理画像用
    public class GetMessageTask extends AsyncTask<String, Integer, Drawable> {
        //料理名初期化
        String next_food_name;
        //pr_url
        String pr_url;
        //food_name
        String food_name;

        //food_flag(一回目参照かどうか
        String pre_food_id="";
        String pr ="";
        JSONObject json;

        @Override
        protected Drawable doInBackground(String... contents) {
            String url="http://www.widret.sakura.ne.jp/cands/index.php/api/v1/reccomend/user_id/0/";
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse res = null;
            String image_url = "";

            try {
                // get.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
                res = httpClient.execute(get);
                String str = EntityUtils.toString(res.getEntity(), "UTF-8");
                json = new JSONObject(str);
                image_url = (String) json.get("next_image_url");
                Drawable img = drawableFromUrl(image_url);
                next_food_name = (String)json.get("next_name");

                food_name = (String)json.get("name");
                pr = (String)json.get("pr");

                Log.d("pr",pr);

                //PR判定
               pre_food_id = json.getString("id");

                Log.d("id",pre_food_id);

                return img;
                // Log.w("test", image_url);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        public void showAlert(final String food_name, final String  url){
            Handler handler = new Handler(); // (1)
            handler.post(new Runnable() {
                @Override
                public void run() {
            new AlertDialog.Builder(MyActivity.this)
                    .setTitle("お得にキャンペーン商品を注文できます")
                    .setMessage(food_name)
                    //.setIcon(R.id.food_image)
                    .setPositiveButton("お得に注文する！", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(url);
                            Intent i = new Intent(Intent.ACTION_VIEW, uri);
                            Log.d("url", url);
                            startActivity(i);
                        }
                    })
                    .setNegativeButton("今回は大丈夫", null)
                    .show();
                }
            });
        }



        @Override
        protected void onPostExecute(Drawable img) {
            // 画像に変換
            try {
                Log.w("test2", "posted!!");
                ImageView imgv = (ImageView) findViewById(R.id.food_image);
                imgv.setImageDrawable(img);
                TextView food_namev = (TextView)findViewById(R.id.food_name);
                food_namev.setText(next_food_name);

                if(!pre_food_id.equals(food_id) && pr.equals("1")){
                    Log.d("test","alert");
                    showAlert(food_name, (String)json.get("pr_url"));
                }

                //pr-idを比較のため更新
                food_id = json.getString("id");

            }catch(Exception e){}
        }
    }

    public void showAlert(){
        Handler handler = new Handler(); // (1)
        handler.post(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MyActivity.this)
                        .setTitle("おめでとう！")
                        .setMessage("目標の設定カロリーを達成しました！")
                                //.setIcon(R.id.food_image)
                        .setNegativeButton("確認", null)
                        .show();
            }
        });
    }

    //ユーザ名用
    public class GetMessageTaskUser extends AsyncTask<String, Integer, Drawable> {
        //料理名初期化
        String name;
        String calorie;
        @Override
        protected Drawable doInBackground(String... contents) {
            String url="http://www.widret.sakura.ne.jp/cands/index.php/api/v1/users/id/0/";
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse res = null;
            String image_url = "";

            try {
                // get.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
                res = httpClient.execute(get);
                String str = EntityUtils.toString(res.getEntity(), "UTF-8");
                JSONObject json = new JSONObject(str);
                name = (String)json.get("name");
                calorie = (String)json.get("target_calorie");

                remaining_cal =  Integer.valueOf(calorie);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(Drawable img) {
            try {

                TextView namev = (TextView)findViewById(R.id.name);
                namev.setText(" "+ name +" さん");
                caloriev = (TextView)findViewById(R.id.calorie);
                caloriev.setText(calorie+" Kcal");
            }catch(Exception e){}
        }
    }

    //天気用
    public class GetMessageTaskWeather extends AsyncTask<String, Integer, Drawable> {
        String temp;
        @Override
        protected Drawable doInBackground(String... contents) {
            String url="http://www.widret.sakura.ne.jp/cands/index.php/api/v1/weather/";
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse res = null;
            String image_url = "";

            try {
                // get.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
                res = httpClient.execute(get);
                String str = EntityUtils.toString(res.getEntity(), "UTF-8");
                JSONObject json = new JSONObject(str);
                image_url = (String) json.get("image_url");
                Drawable img = drawableFromUrl(image_url);
                temp = String.valueOf(json.get("temp"));

                return img;
                // Log.w("test", image_url);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Drawable img) {
            // 画像に変換
            try {
                ImageView imgv = (ImageView) findViewById(R.id.weather_image);
                imgv.setImageDrawable(img);
                TextView tempv = (TextView)findViewById(R.id.temp);
                String temp_short = temp.substring(0,4);
                tempv.setText(temp_short+"℃");

            }catch(Exception e){}
        }
    }

    public static Drawable drawableFromUrl(String url) throws IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(x);
    }

    private void sendNotification(String title, String body) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        int icon = android.R.drawable.sym_def_app_icon;
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, title, when);

        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(this, this.getClass());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, body, contentIntent);
        mNotificationManager.notify(1, notification);
    }
}