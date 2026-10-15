package com.indaya.vistoria;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Canvas;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Base64;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
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
    s.setAllowFileAccessFromFileURLs(true);
    s.setAllowUniversalAccessFromFileURLs(true);
    webView.addJavascriptInterface(new AndroidBridge(),"Android");
    webView.setWebViewClient(new WebViewClient(){
      @Override public void onPageFinished(WebView view,String url){
        super.onPageFinished(view,url);
        try{ view.evaluateJavascript(readAsset("cloud.js"),null); }catch(IOException ignored){}
      }
    });
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

    @JavascriptInterface public void sharePdf(String filename,String json){
      runOnUiThread(()->{
        try{
          JSONObject data=new JSONObject(json==null?"{}":json);
          File dir=new File(getCacheDir(),"pdf"); dir.mkdirs();
          String safe=(filename==null||filename.trim().isEmpty()) ? "vistoria-indaya.pdf" : filename.replaceAll("[^a-zA-Z0-9._-]","_");
          if(!safe.toLowerCase().endsWith(".pdf")) safe += ".pdf";
          File file=new File(dir,safe);

          PdfDocument pdf=new PdfDocument();
          int pageW=595,pageH=842,margin=36;
          int navy=Color.rgb(7,28,61), red=Color.rgb(231,53,53), text=Color.rgb(23,34,56);
          int muted=Color.rgb(103,117,140), line=Color.rgb(224,231,240), green=Color.rgb(21,151,102);
          int amber=Color.rgb(219,139,24), danger=Color.rgb(203,44,44);

          Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
          Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
          stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(1f); stroke.setColor(line);

          int pageNum=1;
          PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(pageW,pageH,pageNum).create());
          Canvas c=page.getCanvas();

          // Header
          p.setStyle(Paint.Style.FILL); p.setColor(navy); c.drawRect(0,0,pageW,104,p);
          Bitmap logo=loadAssetBitmap("indaya_logo.jpg");
          if(logo!=null){
            c.drawBitmap(logo,null,new RectF(margin,18,margin+68,86),p);
          }
          p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(20f);
          c.drawText("INDAYÁ LOCAÇÕES",margin+84,47,p);
          p.setTextSize(12f); p.setTypeface(Typeface.DEFAULT);
          c.drawText("RELATÓRIO DE VISTORIA VEICULAR",margin+84,69,p);
          p.setTextSize(9.5f);
          c.drawText(data.optString("tipo","Vistoria"),margin+84,87,p);
          p.setColor(red); c.drawRect(0,104,pageW,109,p);

          int y=135;
          p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13f);
          c.drawText("DADOS DO VEÍCULO",margin,y,p); y+=10;
          c.drawLine(margin,y,pageW-margin,y,stroke); y+=20;
          p.setTypeface(Typeface.DEFAULT); p.setTextSize(10.5f);
          drawLabelValue(c,p,"Placa",data.optString("placa","-"),margin,y);
          drawLabelValue(c,p,"Marca / Modelo",data.optString("veiculo","-"),300,y); y+=20;
          drawLabelValue(c,p,"Ano",data.optString("ano","-"),margin,y);
          drawLabelValue(c,p,"Cor",data.optString("cor","-"),300,y); y+=20;
          drawLabelValue(c,p,"KM",data.optString("km","-"),margin,y);
          drawLabelValue(c,p,"Combustível",data.optString("combustivel","-"),300,y); y+=20;
          drawLabelValue(c,p,"Data / Hora",data.optString("data","-"),margin,y);
          String status=data.optString("status","-");
          drawLabelValue(c,p,"Status",status,300,y); y+=26;

          JSONObject cliente=data.optJSONObject("cliente");
          if(cliente!=null && cliente.length()>0){
            p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13f); p.setColor(text);
            c.drawText("DADOS DO LOCATÁRIO",margin,y,p); y+=10;
            c.drawLine(margin,y,pageW-margin,y,stroke); y+=20;
            p.setTypeface(Typeface.DEFAULT); p.setTextSize(10.5f);
            drawLabelValue(c,p,"Nome",cliente.optString("nome","-"),margin,y);
            drawLabelValue(c,p,"CPF",cliente.optString("cpf","-"),300,y); y+=20;
            drawLabelValue(c,p,"Telefone",cliente.optString("telefone","-"),margin,y);
            drawLabelValue(c,p,"CNH",cliente.optString("cnh","-"),300,y); y+=26;
          }

          p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13f); p.setColor(text);
          c.drawText("CHECKLIST DA VISTORIA",margin,y,p); y+=10;
          c.drawLine(margin,y,pageW-margin,y,stroke); y+=12;

          JSONObject checks=data.optJSONObject("checks");
          if(checks!=null){
            JSONArray names=checks.names();
            if(names!=null){
              for(int i=0;i<names.length();i++){
                String k=names.optString(i),v=checks.optString(k,"-");
                p.setStyle(Paint.Style.FILL); p.setColor(i%2==0?Color.rgb(248,250,253):Color.WHITE);
                c.drawRect(margin,y-1,pageW-margin,y+22,p);
                p.setTextSize(10f); p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(k,margin+8,y+14,p);
                int sc=v.equals("OK")?green:(v.contains("Aten"))?amber:danger;
                p.setColor(sc); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(v,pageW-margin-90,y+14,p);
                y+=23;
              }
            }
          }
          y+=10;

          p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13f);
          c.drawText("OBSERVAÇÕES",margin,y,p); y+=15;
          p.setTypeface(Typeface.DEFAULT); p.setTextSize(10f); p.setColor(muted);
          y=drawWrappedText(c,p,data.optString("observacoes","Sem observações"),margin,y,pageW-(margin*2),14);

          y+=10;
          p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(13f);
          c.drawText("ITENS DE ATENÇÃO",margin,y,p); y+=15;
          p.setTypeface(Typeface.DEFAULT); p.setTextSize(10f); p.setColor(muted);
          y=drawWrappedText(c,p,data.optString("atencao","Nenhum"),margin,y,pageW-(margin*2),14);

          y+=14;
          p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(11f);
          c.drawText("Responsável pela vistoria:",margin,y,p);
          p.setTypeface(Typeface.DEFAULT); p.setColor(muted);
          c.drawText(data.optString("responsavel","-"),margin+155,y,p);
          y+=18;

          Bitmap sig=decodeDataUrl(data.optString("assinatura",""));
          if(sig!=null && y+85<pageH-35){
            p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(11f);
            c.drawText("Assinatura",margin,y,p); y+=8;
            RectF dst=new RectF(margin,y,260,y+65);
            c.drawBitmap(sig,null,dst,p);
            c.drawLine(margin,y+67,260,y+67,stroke);
            y+=78;
          }

          drawFooter(c,p,pageNum,pageW,pageH,navy,muted);
          pdf.finishPage(page);

          JSONArray photos=data.optJSONArray("photos");
          JSONArray labels=data.optJSONArray("photoLabels");
          if(photos!=null && photos.length()>0){
            int index=0;
            while(index<photos.length()){
              pageNum++;
              page=pdf.startPage(new PdfDocument.PageInfo.Builder(pageW,pageH,pageNum).create());
              c=page.getCanvas();
              p.setStyle(Paint.Style.FILL); p.setColor(navy); c.drawRect(0,0,pageW,70,p);
              if(logo!=null)c.drawBitmap(logo,null,new RectF(margin,10,margin+50,60),p);
              p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(17f);
              c.drawText("REGISTRO FOTOGRÁFICO",margin+64,35,p);
              p.setTextSize(10f); p.setTypeface(Typeface.DEFAULT);
              c.drawText(data.optString("placa",""),margin+64,53,p);

              float gap=16f, boxW=(pageW-(margin*2)-gap)/2f, boxH=290f;
              for(int slot=0;slot<4 && index<photos.length();slot++,index++){
                int row=slot/2,col=slot%2;
                float left=margin+col*(boxW+gap);
                float top=92+row*(boxH+28);
                String label=(labels!=null&&index<labels.length())?labels.optString(index):("Foto "+(index+1));
                Bitmap bm=decodeDataUrl(photos.optString(index,""));

                p.setColor(text); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(10.5f);
                c.drawText(label,left,top-8,p);
                p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE); c.drawRoundRect(new RectF(left,top,left+boxW,top+boxH),8,8,p);
                c.drawRoundRect(new RectF(left,top,left+boxW,top+boxH),8,8,stroke);
                if(bm!=null) drawBitmapCenterCrop(c,bm,new RectF(left+3,top+3,left+boxW-3,top+boxH-3),p);
              }
              drawFooter(c,p,pageNum,pageW,pageH,navy,muted);
              pdf.finishPage(page);
            }
          }

          try(FileOutputStream out=new FileOutputStream(file)){ pdf.writeTo(out); }
          pdf.close();
          sharePdfFile(file);
        }catch(Exception e){
          Toast.makeText(MainActivity.this,"Não foi possível gerar o PDF completo.",Toast.LENGTH_LONG).show();
        }
      });
    }

    private void sharePdfFile(File file){
      Uri uri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".fileprovider",file);
      Intent send=new Intent(Intent.ACTION_SEND);
      send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM,uri);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      try{
        Intent whats=new Intent(send);
        whats.setPackage("com.whatsapp");
        startActivity(whats);
      }catch(ActivityNotFoundException e){
        try{
          Intent business=new Intent(send);
          business.setPackage("com.whatsapp.w4b");
          startActivity(business);
        }catch(ActivityNotFoundException ex){
          startActivity(Intent.createChooser(send,"Compartilhar relatório em PDF"));
        }
      }
    }
  }

  private Bitmap loadAssetBitmap(String name){
    try(InputStream in=getAssets().open(name)){ return BitmapFactory.decodeStream(in); }
    catch(Exception e){ return null; }
  }

  private Bitmap decodeDataUrl(String data){
    try{
      if(data==null||data.isEmpty())return null;
      int comma=data.indexOf(',');
      String b64=comma>=0?data.substring(comma+1):data;
      byte[] bytes=Base64.decode(b64,Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }catch(Exception e){ return null; }
  }

  private void drawLabelValue(Canvas c,Paint p,String label,String value,float x,float y){
    int old=p.getColor(); float oldSize=p.getTextSize(); Typeface oldType=p.getTypeface();
    p.setColor(Color.rgb(103,117,140)); p.setTextSize(8.5f); p.setTypeface(Typeface.DEFAULT_BOLD);
    c.drawText(label.toUpperCase(Locale.ROOT),x,y-7,p);
    p.setColor(Color.rgb(23,34,56)); p.setTextSize(10.5f); p.setTypeface(Typeface.DEFAULT);
    c.drawText(value==null||value.isEmpty()?"-":value,x,y+7,p);
    p.setColor(old); p.setTextSize(oldSize); p.setTypeface(oldType);
  }

  private int drawWrappedText(Canvas c,Paint p,String text,float x,int y,float maxWidth,int lineHeight){
    if(text==null||text.trim().isEmpty())text="-";
    for(String para:text.split("\\n")){
      String[] words=para.trim().split("\\s+");
      String line="";
      for(String w:words){
        String test=line.isEmpty()?w:line+" "+w;
        if(p.measureText(test)>maxWidth && !line.isEmpty()){
          c.drawText(line,x,y,p); y+=lineHeight; line=w;
        }else line=test;
      }
      if(!line.isEmpty()){c.drawText(line,x,y,p);y+=lineHeight;}
    }
    return y;
  }

  private void drawBitmapCenterCrop(Canvas c,Bitmap bm,RectF dst,Paint p){
    float srcRatio=bm.getWidth()/(float)bm.getHeight();
    float dstRatio=dst.width()/dst.height();
    int sw=bm.getWidth(),sh=bm.getHeight(),sx=0,sy=0;
    if(srcRatio>dstRatio){sw=Math.round(sh*dstRatio);sx=(bm.getWidth()-sw)/2;}
    else {sh=Math.round(sw/dstRatio);sy=(bm.getHeight()-sh)/2;}
    android.graphics.Rect src=new android.graphics.Rect(sx,sy,sx+sw,sy+sh);
    c.drawBitmap(bm,src,dst,p);
  }

  private void drawFooter(Canvas c,Paint p,int pageNum,int pageW,int pageH,int navy,int muted){
    p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(243,246,251)); c.drawRect(0,pageH-30,pageW,pageH,p);
    p.setTextSize(8.5f); p.setTypeface(Typeface.DEFAULT); p.setColor(muted);
    c.drawText("Indayá Locações - Relatório de vistoria veicular",36,pageH-12,p);
    c.drawText("Página "+pageNum,pageW-88,pageH-12,p);
  }

  @Override public void onBackPressed(){
    if(webView!=null&&webView.canGoBack()) webView.goBack(); else super.onBackPressed();
  }
}