package com.indaya.vistoria;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER_REQUEST=1001;
  private static final int CAMERA_PERMISSION_REQUEST=1002;
  private WebView webView;
  private ValueCallback<Uri[]> fileCallback;
  private Uri cameraUri;

  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    webView=new WebView(this);
    setContentView(webView);
    WebSettings s=webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    webView.addJavascriptInterface(new AndroidBridge(),"Android");
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){
        if(fileCallback!=null) fileCallback.onReceiveValue(null);
        fileCallback=cb;
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
          ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST);
        } else openChooser();
        return true;
      }
    });
    try{
      String html=readAsset("index1.part")+readAsset("index2.part")+readAsset("index3.part")+readAsset("index4.part");
      webView.loadDataWithBaseURL("file:///android_asset/",html,"text/html","UTF-8",null);
    }catch(IOException e){
      Toast.makeText(this,"Erro ao abrir o aplicativo.",Toast.LENGTH_LONG).show();
    }
  }

  private String readAsset(String name)throws IOException{
    try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[8192]; int n;
      while((n=in.read(buf))>0) out.write(buf,0,n);
      return out.toString(StandardCharsets.UTF_8.name());
    }
  }

  private void openChooser(){
    Intent gallery=new Intent(Intent.ACTION_GET_CONTENT);
    gallery.addCategory(Intent.CATEGORY_OPENABLE);
    gallery.setType("image/*");
    Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    Intent[] initial=new Intent[0];
    if(camera.resolveActivity(getPackageManager())!=null){
      try{
        File f=createImageFile();
        cameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
        camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        initial=new Intent[]{camera};
      }catch(IOException ignored){}
    }
    Intent chooser=new Intent(Intent.ACTION_CHOOSER);
    chooser.putExtra(Intent.EXTRA_INTENT,gallery);
    chooser.putExtra(Intent.EXTRA_TITLE,"Adicionar foto");
    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,initial);
    startActivityForResult(chooser,FILE_CHOOSER_REQUEST);
  }

  private File createImageFile()throws IOException{
    String ts=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
    File dir=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    if(dir!=null&&!dir.exists()) dir.mkdirs();
    return File.createTempFile("INDAYA_"+ts+"_",".jpg",dir);
  }

  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=FILE_CHOOSER_REQUEST||fileCallback==null) return;
    Uri[] result=null;
    if(resultCode==RESULT_OK){
      if(data!=null&&data.getData()!=null) result=new Uri[]{data.getData()};
      else if(cameraUri!=null) result=new Uri[]{cameraUri};
    }
    fileCallback.onReceiveValue(result);
    fileCallback=null; cameraUri=null;
  }

  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
    super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    if(requestCode==CAMERA_PERMISSION_REQUEST) openChooser();
  }

  public class AndroidBridge {
    @JavascriptInterface public void shareText(String text){
      runOnUiThread(()->{
        Intent i=new Intent(Intent.ACTION_SEND);
        i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,text);
        startActivity(Intent.createChooser(i,"Compartilhar vistoria"));
      });
    }
    @JavascriptInterface public void exportJson(String json,String filename){
      runOnUiThread(()->{
        try{
          File dir=new File(getCacheDir(),"exports"); dir.mkdirs();
          File f=new File(dir,filename);
          try(FileOutputStream out=new FileOutputStream(f)){out.write(json.getBytes(StandardCharsets.UTF_8));}
          Uri u=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",f);
          Intent i=new Intent(Intent.ACTION_SEND);
          i.setType("application/json"); i.putExtra(Intent.EXTRA_STREAM,u);
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          startActivity(Intent.createChooser(i,"Salvar ou compartilhar backup"));
        }catch(Exception e){Toast.makeText(MainActivity.this,"Erro ao exportar backup.",Toast.LENGTH_LONG).show();}
      });
    }
    @JavascriptInterface public void printPage(){
      runOnUiThread(()->{
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
        PrintDocumentAdapter ad=webView.createPrintDocumentAdapter("Indaya_Vistoria");
        pm.print("Indaya_Vistoria",ad,new PrintAttributes.Builder().build());
      });
    }
  }

  @Override public void onBackPressed(){
    if(webView!=null&&webView.canGoBack()) webView.goBack(); else super.onBackPressed();
  }
}