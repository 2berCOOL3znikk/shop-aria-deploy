package com.aria.service.analytics;
import java.util.*;
/** Rolling-origin validation of a small supervised autoregression; never a claim of causal demand. */
public final class DemandForecast {
 private DemandForecast(){}
 public record Result(String method,String confidence,double expectedUnits,int horizonDays,Double validationMae,Double baselineMae,
                      int historyDays,long recordedUnits,List<Double> daily,String limitation){
  /** Compatibility for the original 14-day Java caller; new callers use expectedUnits and horizonDays. */
  @Deprecated public double units14(){return expectedUnits;}
  @com.fasterxml.jackson.annotation.JsonProperty("units14") public Double legacyFourteenDayTotal(){return horizonDays==14?expectedUnits:null;}
 }
 public static Result fit(double[] observations){return fit(observations,14);}
 public static Result fit(double[] observations,int horizon){
  if(horizon<1||horizon>365)throw new IllegalArgumentException("Неверный горизонт прогноза");
  double[] y=observations.clone();for(double v:y)if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("Неверная история отгрузок");
  int n=y.length;long total=Math.round(Arrays.stream(y).sum());
  if(n<14||total==0)return new Result("INSUFFICIENT_HISTORY","LOW",0,horizon,null,null,n,total,List.of(),"Недостаточно подтверждённых отгрузок. Ноль в данных не доказывает отсутствие спроса; запас по прогнозу не назначается.");
  double modelError=0,baseError=0;boolean trained=n>=84&&Arrays.stream(y,0,n-14).filter(v->v>0).count()>=28;
  if(trained){for(int t=n-14;t<n;t++){double[] prefix=Arrays.copyOf(y,t);double[] beta=train(prefix);modelError+=Math.abs(y[t]-predict(prefix,beta));baseError+=Math.abs(y[t]-mean(prefix));}modelError/=14;baseError/=14;}
  boolean useModel=trained&&Double.isFinite(modelError)&&baseError>0&&modelError<baseError*.9;
  double[] buffer=Arrays.copyOf(y,n+horizon);double[] beta=useModel?train(y):null;List<Double> daily=new ArrayList<>();double sum=0;
  for(int step=0;step<horizon;step++){double[] prefix=Arrays.copyOf(buffer,n+step);double value=useModel?predict(prefix,beta):mean(prefix);value=Math.max(0,Math.min(value,Math.max(1,Arrays.stream(y).max().orElse(1))*3));buffer[n+step]=value;daily.add(round(value));sum+=value;}
  return new Result(useModel?"VALIDATED_RIDGE_AR":"MOVING_AVERAGE_7","LOW",round(sum),horizon,trained?round(modelError):null,trained?round(baseError):null,n,total,daily,
   (useModel?"Обучена ridge-авторегрессия на лагах 1/7 и средней 7 дней. Последовательный прогноз следующего дня на последних 14 днях улучшил MAE базовой модели минимум на 10%. ":"Используется средняя подтверждённых отгрузок за последние 7 дней; это статистический ориентир, не обученная ML-модель. ")+"История наличия и потерянного спроса неизвестна. Нулевые дни могут быть отсутствием товара. Прогноз отгрузок, не гарантия спроса; сезонность за пределами недели не восстановлена.");
 }
 private static double round(double v){return Math.round(v*100d)/100d;}
 private static double mean(double[] y){return Arrays.stream(y,Math.max(0,y.length-7),y.length).average().orElse(0);}
 private static double[] features(double[] y,int t){double sum=0;for(int j=t-7;j<t;j++)sum+=y[j];return new double[]{1,y[t-1],y[t-7],sum/7};}
 private static double[] train(double[] y){
  double[][] a=new double[4][5];for(int t=7;t<y.length;t++){var x=features(y,t);for(int i=0;i<4;i++){a[i][4]+=x[i]*y[t];for(int j=0;j<4;j++)a[i][j]+=x[i]*x[j];}}
  for(int i=1;i<4;i++)a[i][i]+=10; // Fixed regularization, no tuning on held-out data.
  for(int col=0;col<4;col++){int pivot=col;for(int i=col+1;i<4;i++)if(Math.abs(a[i][col])>Math.abs(a[pivot][col]))pivot=i;var row=a[col];a[col]=a[pivot];a[pivot]=row;if(Math.abs(a[col][col])<1e-10)return new double[]{mean(y),0,0,0};double scale=a[col][col];for(int j=col;j<5;j++)a[col][j]/=scale;for(int i=0;i<4;i++)if(i!=col){double k=a[i][col];for(int j=col;j<5;j++)a[i][j]-=k*a[col][j];}}
  return new double[]{a[0][4],a[1][4],a[2][4],a[3][4]};
 }
 private static double predict(double[] y,double[] beta){var x=features(y,y.length);double v=0;for(int i=0;i<4;i++)v+=x[i]*beta[i];return Math.max(0,v);}
}
