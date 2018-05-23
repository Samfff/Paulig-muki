package com.muki;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Canvas;

import com.muki.core.MukiCupApi;
import com.muki.core.MukiCupCallback;
import com.muki.core.model.Action;
import com.muki.core.model.DeviceInfo;
import com.muki.core.model.ErrorCode;
import com.muki.core.model.ImageProperties;
import com.muki.core.util.ImageUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText mSerialNumberEdit;
    private TextView mCupIdText;
    private TextView mDeviceInfoText;
    private SeekBar mContrastSeekBar;
    private ProgressDialog mProgressDialog;
    private EditText mTwitterUsernameEdit;
    private TextView mTwitterUsernameText;
    // DEBUG
    private TextView mDebugLogText;
    private ImageView mDebugImage;


    private String mCupId;
    private String mTwitterUsername;
    private MukiCupApi mMukiCupApi;
    private Bitmap mImage;


    public enum RSSXMLTag {
        TITLE, CREATOR, IGNORETAG, DESCRIPTION;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mImage = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Loading. Please wait...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mMukiCupApi = new MukiCupApi(getApplicationContext(), new MukiCupCallback() {
            @Override
            public void onCupConnected() {
                showToast("Cup connected");
            }

            @Override
            public void onCupDisconnected() {
                showToast("Cup disconnected");
            }

            @Override
            public void onDeviceInfo(DeviceInfo deviceInfo) {
                hideProgress();
                mDeviceInfoText.setText(deviceInfo.toString());
            }

            @Override
            public void onImageCleared() {
                showToast("Image cleared");
            }

            @Override
            public void onImageSent() {
                showToast("Image sent");
            }

            @Override
            public void onError(Action action, ErrorCode errorCode) {
                showToast("Error:" + errorCode + " on action:" + action);
            }
        });

        mSerialNumberEdit = (EditText) findViewById(R.id.serailNumberText);
        mCupIdText = (TextView) findViewById(R.id.cupIdText);
        mDeviceInfoText = (TextView) findViewById(R.id.deviceInfoText);
        mTwitterUsernameEdit = (EditText) findViewById(R.id.twitterUsernameEdit);
        mTwitterUsernameText = (TextView) findViewById(R.id.twitterUsernameText);
        mDebugLogText = (TextView) findViewById(R.id.debugLogText);
        mDebugImage = (ImageView) findViewById(R.id.debugImageView);

        /*reset(null);*/
    }

    public void send(View view) {
        showProgress();

        new AsyncTask<String, Void, TwitterFeed>() {
            @Override
            protected TwitterFeed doInBackground(String... strings) {
                try {
                    /*String serialNumber = strings[0];
                    return MukiCupApi.cupIdentifierFromSerialNumber(serialNumber);*/
                    TwitterFeed tf = new TwitterFeed(strings[0]);
                    tf.getFeed();
                    return tf;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(TwitterFeed tf) {
                if (tf != null) {
                    mTwitterUsername = tf.username;
                    mTwitterUsernameText.setText(mTwitterUsername);
                    Tweet tweet;
                    if (tf.tweets.size() == 1) {
                    /*if (false) {*/
                        tweet = tf.tweets.get(0);
                    } else {
                        tweet = tf.tweets.get(1);
                    }
                    /*mDebugLogText.setText(tweet.postCreator + "\n\n" + tweet.postTitle + "\n\n" + tweet.postDescription);*/
                    /*mMukiCupApi.sendImage(tweetToImage(tf.tweets.get(0)), new ImageProperties(0), mCupId);*/
                    mDebugImage.setImageBitmap(tweetToImage(tweet));
                }
                else {
                    mDebugLogText.setText("Something went wrong");
                }

                hideProgress();
            }
        }.execute(mTwitterUsernameEdit.getText().toString());
    }

    public void request(View view) {
        String serialNumber = mSerialNumberEdit.getText().toString();
        showProgress();
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    String serialNumber = strings[0];
                    return MukiCupApi.cupIdentifierFromSerialNumber(serialNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                mCupId = s;
                mCupIdText.setText(mCupId);
                hideProgress();
            }
        }.execute(serialNumber);
    }

    public void deviceInfo(View view) {
        showProgress();
        mMukiCupApi.getDeviceInfo(mCupId);
    }

    private void showToast(final String text) {
        hideProgress();
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private Bitmap tweetToImage(Tweet tweet) {

        String tweetString = tweet.postDescription;
        float textSize = 15;

        if (tweetString.length() > 100) {
            for (int i = 1; i < 10; i++) {
                tweetString = new StringBuilder(tweetString).insert((tweetString.length() - 1) / i, "\n").toString();
            }
        } else if (tweetString.length() < 50) {
            for (int i = 1; i < 6; i++) {
                tweetString = new StringBuilder(tweetString).insert((tweetString.length() - 1) / i, "\n").toString();
            }
        } else {
            for (int i = 1; i < 8; i++ ) {
                tweetString = new StringBuilder(tweetString).insert((tweetString.length() - 1) / i, "\n").toString();
            }
        }
        String tweetAsString = tweet.postCreator.toString() + "\n\n" + tweetString;

        Bitmap newImage = Bitmap.createBitmap(296, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newImage);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        canvas.drawText(tweetAsString, 20, 30, paint);
        /*return newImage;*/
        return mImage;
    }

    private void showProgress() {
        mProgressDialog.show();
    }

    private void hideProgress() {
        mProgressDialog.dismiss();
    }
}

