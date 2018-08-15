package es.resultados.fft;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ca.uol.aig.fftpack.RealDoubleFFT;




public class FFTSpectralAnalyzer extends Activity implements OnClickListener {
	

	//Ojeto de tipo WakeLock que permite mantener despierta la aplicacion
	protected PowerManager.WakeLock wakelock;
	

	RecordAudio recordTask; // proceso de grabacion y analisis
	AudioRecord audioRecord; // objeto de la clase AudioReord que permite captar el sonido
	
	Button startStopButton; // boton de arranquue y pausa
	boolean started = false; // condicion del boton
	
	
	
	// Configuracion del canal de audio
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	static double RATE = 44100; // frecuencia o tasa de muestreo
	
	
	int bufferSize = 0;  // tamaño del buffer segun la configuracion de audio
	int bufferReadResult = 0; // tamaño de la lectura
	int blockSize_buffer = 4096; // valor por defecto para el bloque de lectura de buffer
	
	
	
	// Objeto de la clase que determina la FFT de un vector de muestras
	private RealDoubleFFT transformer;
	int blockSize_fft = 8192;// tamaño de la transformada de Fourier
	
	
	// Frecuencias del rango de estudio asociadas al instrumento
	static double MIN_FREQUENCY = 30; // HZ
	static double MAX_FREQUENCY = 20000; // HZ
	
		
	// Valores pordefecto para el estudio de los armonicos
  
	double UMBRAL = 100; // umbral de amplitud valido para tener en cuenta 
						 // los armonicos, depende del tamaño de la FFT
	
	int LONGTRAMA = 20; // tamaño de la ventana de estudio de los armonicos 
						// tambien depende del tamaño de la FFT
	
	int NUM_ARMONICOS = 6; // numero de armonicos a tener en cuenta 
	

	
	double[] aux3;{ // declaracion de vector auxiliar para el estudio de la trama
	aux3 = new double[LONGTRAMA];} // sera el array que contenga la amplitud de los armonicos
	
	double [] validos = new double[NUM_ARMONICOS] ; // vector que tendra solo los armonicos de interes

	

	

	
	// Elementos para la representacion en pantalla



    //int alturaGrafica = 600; // tamaño vertical de la grafica
	//int blockSize_grafica = 1900; // tamaño horizontal de la grafica

	// Calculamos el cociente de la Relacion de Aspecto que usaremos para ubicar
	// todo aquello cuya posicion varie en funcion de un valor determinado
	//int factor = (int) Math.round((double)blockSize_grafica/(double)alturaGrafica); //adptativo

	// Tamaños de texto para los diferentes mensajes y resultados
	int TAM_TEXT = 40;
	//int TAM_TEXT1 = 10*factor;
	//int TAM_TEXT2 = 5*factor;


	
	TextView statusText; // objeto de la clase TextView para mostrar mensaje
	
	ImageView imageView; // imagen para la representacion del espectro
	Bitmap bitmap;
	Canvas canvas; 
	Paint paint;
	
	ImageView imageView2; // imagen para dibujar las bandas de frecuencia
	Bitmap bitmap2;
	Canvas canvas2; 
	Paint paint2;
	
	Canvas canvas3;// para dibujar el valor de la SNR
	Paint paint3;
	
	Canvas canvas4; // para dibujar texto (frecuencia) en el espectrograma
	Paint paint4;
	
	Canvas canvas5; // para dibujar el promedio de la magnitud de los armonicos en el espectrograma
	Paint paint5;
	
	Canvas canvas6; // para dibujar el umbral establecido por el usuario
	Paint paint6;
	
	
	/// PREFERENCIAS

	
	int altura_umbral = 7;
	
	// Usamos la clase DecimalFormat para establecer el numero de decimales del resultado
	DecimalFormat df1;
	DecimalFormatSymbols symbols = new  DecimalFormatSymbols(Locale.US);{
    symbols.setDecimalSeparator('.');
    df1= new DecimalFormat("#.#",symbols);}
	
    // Cuando la actividad es llamada por primera vez 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graficas);
        
        // Inicializacion de todos los elementos graficos
        statusText = (TextView) this.findViewById(R.id.StatusTextView);
        startStopButton = (Button) this.findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(this);

		int[] escalaPantalla; // eje X , Y , relacion de dimensiones.
		escalaPantalla = screenDimension();

		// Tamaños de texto para los diferentes mensajes y resultados
		int TAM_TEXT1 = 10*escalaPantalla[2];



		 // imagen para la representacion del espectro
		imageView = (ImageView) this.findViewById(R.id.ImageView01);


		bitmap = Bitmap.createBitmap(escalaPantalla[0], escalaPantalla[1],
				Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		imageView.setImageBitmap(bitmap);
		
		// imagen para dibujar las bandas de frecuencia


		imageView2 = (ImageView) this.findViewById(R.id.ImageView02);
		bitmap2 = Bitmap.createBitmap((int) escalaPantalla[0], TAM_TEXT1,
				Bitmap.Config.ARGB_8888);
		canvas2 = new Canvas(bitmap2);
		paint2 = new Paint();
		paint2.setColor(Color.WHITE);
		imageView2.setImageBitmap(bitmap2);
		
		
		// para dibujar el valor de la SNR
		canvas3 = new Canvas(bitmap); 
		paint3 = new Paint();
		paint3.setColor(Color.MAGENTA);
		
		// para dibujar texto (frecuencia) en el espectrograma
		canvas4 = new Canvas(bitmap); 
		paint4 = new Paint();
		paint4.setColor(Color.YELLOW);
		
		// para dibujar el promedio de la magnitud de los armonicos en el espectrograma
		canvas5 = new Canvas(bitmap);
		paint5 = new Paint();
		paint5.setColor(Color.RED);
		
		 // para dibujar el umbral establecido por el usuario
		canvas6 = new Canvas(bitmap);  
		paint6 = new Paint();
		paint6.setColor(Color.CYAN);
		
		
		//evitar que la pantalla se apague
        final PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
        this.wakelock=pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "etiqueta");
        wakelock.acquire();
		
        
        // Dibuja el eje de frecuencias
        DibujaEjeFrecuencias();

    }
    
    ////////////////////////////////////////////////////////////////////////
	// Hace que la pantalla siga encendida hasta que la actividad termine
	protected void onDestroy(){
	        super.onDestroy();
	        
	        this.wakelock.release();
	        
	 }
	
	// Adicionalmente, se recomienda usar onResume, y onSaveInstanceState, para que, 
	// si minimizamos la aplicacion, la pantalla se apague normalmente, de lo 
	// contrario, no se apagará la pantalla aunque no tengamos a nuestra aplicación 
	// en primer plano.

	protected void onResume(){
	        super.onResume();
	        wakelock.acquire();
	        // Valor que muestra el boton al volver a la actividad
	        startStopButton.setText("ON");
	 }
    
	// Si se sale de la actividad de manera inesperada
    @Override
    protected void onPause() {
        super.onPause();
        if(started) {
        	      
        	started = false;          
        }        
    }
    
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        this.wakelock.release();	    

    }
    
    
    // PROCESO O TAREA ASINCRONA QUE SENCARGARA DE RECOGER Y ANALIZAR LA SEÑAL DE AUDIO DE ENTRADA
    private class RecordAudio extends AsyncTask<Void, short[], Void> {
    	
		@Override
		protected Void doInBackground(Void... params) {
			try {



				// estimacion del tamaño del buffer en funcion de la configuracion de audio
				bufferSize = AudioRecord.getMinBufferSize((int)RATE,
						channelConfiguration, audioEncoding);
				
				// inicializacion del objeto de la clase AudioRecord 
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC,(int) RATE,
						channelConfiguration, audioEncoding, bufferSize);

				// declaracion del vector que almacenara primero los datos recogidos del microfono
				short[] audio_data = new short[blockSize_buffer]; // tipo de dato short (2^15 = 32768)

				audioRecord.startRecording(); // empieza a grabar

				while (started) { // mientras no se pulse de nuevo el boton
					
					// tamañod de la lectura
					bufferReadResult = audioRecord.read(audio_data, 0, blockSize_buffer);
					
					
					// se mandan las muestras recogidas para su procesado
					publishProgress(audio_data);
				
				}
				
				audioRecord.stop(); // para la grabacion momentaneamente
				
			} catch (Throwable t) { // en caso de error, p.ej. captura de audio ya activa
				Log.e("AudioRecord", "Recording Failed");
			}

			return null;
		}

		protected void onProgressUpdate(short[]... toTransform) {

			double maximo = 0,promedio = 0, varianza = 0;


			//pasa valores de pantalla para no tener que entrar de nuevo a cada funcion

			int[] escalaPantalla; // eje X , Y , relacion de dimensiones.
			escalaPantalla = screenDimension();


			// Arrays con las muestras de audio en tiempo y frecuencia en formato double
			double[] trama, trama_espectro;	
			trama = new double[blockSize_fft];
			
			// inicializamos el vector que contendra la FFT de
			transformer = new RealDoubleFFT(blockSize_fft);

			
			for (int i = 0; i < bufferReadResult; i++) {

				trama[i] = (double) toTransform[0][i];
				//trama[i * 2 + 1] = 0; // aumentaremos la resolucion en frecuencia de la transformada interpolando ceros

			}
			
			maximo = max(trama,0,trama.length).valor;
			
			//promedio = promedio(trama);
			
			// normalizamos la trama de sonido dividiendo todas las muestra por la de mayor valor
			normaliza(trama);
			
			varianza = varianza(trama);
			


			// Conseguimos precision con el enventanado
			// Filtra los armonicos en el espectro
			// Destaca y realza los fundamentales
				
			trama = aplicaHamming(trama); 
					
					
			// Dominio transformado. Realiza la FFT de la trama
			transformer.ft(trama);
			
			statusText.setTextSize(TAM_TEXT); // definimos el tamaño para el texto
			


			


			DibujaEjeFrecuencias(); // Dibuja las bandas que componen el eje de frecuencias			
			

			// Normalizamos el espectro para su representacion
			trama_espectro = normaliza(trama);
			
						
			DibujaEspectro(trama_espectro, escalaPantalla); // representa graficamente el espectro de la señal
			
			// Dibuja una linea roja que representa el promedio del espectro
			//canvas5.drawLine(0, alturaGrafica -(float)promedio(trama_espectro)*alturaGrafica, 
			//		blockSize_grafica,alturaGrafica -(float)promedio(trama_espectro)*alturaGrafica, paint5);

			
			// Dibuja linea cyan con el umbral seleccionado por el usuario
			canvas6.drawLine(0, escalaPantalla[1] - altura_umbral, escalaPantalla[0],escalaPantalla[1] -altura_umbral, paint6);
			
			
			EscribirArmonicos(escalaPantalla);
		
			
			
		}
	}





    public void EscribirArmonicos(int[] escala){
		int TAM_TEXT2 = 5*escala[2];
		paint4.setAntiAlias(true);
		paint4.setFilterBitmap(true);
		paint4.setTextSize(TAM_TEXT2);
    }



    ///////////////////////////////////////////////////////////////////////////////
	//DIBUJA EL EJE DE FRECUENCIAS/////////////////////////////////////////////////
    public void DibujaEjeFrecuencias(){
    	
		int[] escalaPantalla=screenDimension();
		canvas2.drawColor(Color.BLACK);
		paint2.setAntiAlias(true);
		paint2.setFilterBitmap(true);
		
		// Valores que se mostrara en el eje X
		int[]bandas ={30,60,125,250,500,1000,2000,4000,8000,16000};
		paint2.setStrokeWidth(5);
		canvas2.drawLine(0,0,escalaPantalla[0],0,paint2);
		int factor1=escalaPantalla[0]/3;
		int TAM_TEXT3 = 7*escalaPantalla[2];
		int TAM_TEXT1 = 10*escalaPantalla[2];


		paint2.setTextSize(TAM_TEXT3);

		for(int i=0; i < bandas.length;i++){

            canvas2.drawText(String.valueOf(bandas[i]),Math.round(factor1*Math.log10(bandas[i]))-(TAM_TEXT3)/2-Math.round(factor1*Math.log10(bandas[0]))+50,TAM_TEXT3,paint2);
        }
		/*canvas2.drawText(String.valueOf(bandas[0]),Math.round(factor1*Math.log10(bandas[0]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[1]),Math.round(factor1*Math.log10(bandas[1]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[2]),Math.round(factor1*Math.log10(bandas[2]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[3]),Math.round(factor1*Math.log10(bandas[3]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[4]),Math.round(factor1*Math.log10(bandas[4]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[5]),Math.round(factor1*Math.log10(bandas[5]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[6]),Math.round(factor1*Math.log10(bandas[6]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[7]),Math.round(factor1*Math.log10(bandas[7]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[8]),Math.round(factor1*Math.log10(bandas[8]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[9]),Math.round(factor1*Math.log10(bandas[9]))-(TAM_TEXT3)/2,TAM_TEXT3,paint2);*/
		canvas2.drawText("Hz",escalaPantalla[0] - TAM_TEXT1,TAM_TEXT3,paint2);

		imageView2.invalidate();
		
		
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////
	//DIBUJA EL ESPECTRO///////////////////////////////////////////////////////////
    public void DibujaEspectro(double[] trama_espectro, int[] escala){
    	
    	// Claculo del la relacion Señal a Ruido (dB)
		// Resulta del cociente entre el valor maximo del espectro entre el pormedio
		// Lo ideal es que la SNR valga infinio, lo que significa que no hay ruido
		//double snr2 = 10*Math.log10(max(trama_espectro,0,trama_espectro.length).valor/promedio(trama_espectro));
		int factor1=escala[0]/3;
		int TAM_TEXT3 = 7*escala[2];
		int x;
		int downy ;
		int upy;
		canvas.drawColor(Color.BLACK);



		for (int i = 0; i < trama_espectro.length; i++) {


			 x = (int) (Math.round(factor1*Math.log10(i*2.69))-(TAM_TEXT3)/2-Math.round(factor1*Math.log10(30))+80);
			 downy = (int) (escala[1] - (trama_espectro[i]*escala[1]));
			 upy = escala[1];
			 canvas.drawLine(x, downy, x, upy, paint);
			for (int j=x; j < (Math.round(factor1*Math.log10((i+1)*2.69))-(TAM_TEXT3)/2-Math.round(factor1*Math.log10(30))+80);j++){

				downy = (int) (escala[1] - (trama_espectro[i]*escala[1]));
				upy = escala[1];
				canvas.drawLine(j, downy, j, upy, paint);
			}
		}
		
		//paint3.setAntiAlias(true);
		//paint3.setFilterBitmap(true);
		//paint3.setTextSize(TAM_TEXT2);
		//canvas3.drawText(" SNR: " + df1.format(snr2) + " dB", blockSize_grafica-alTuraGrafica, TAM_TEXT3, paint3);
		
		imageView.invalidate();
		
    }
    


	
	// Metodo para el calculo del promedio de un vector de muestras.
    
    private static double promedio(double[] datos) {
        
        int N = datos.length;
        double med = 0;
        for (int k = 0; k < N; k++) {
            
            med += Math.abs(datos[k]);
        }
        med = med / N;
        return med;
    }
    
    // Metodo para el calculo de la media de un vector de muestras.
    
    private static double media(double[] datos) {
        // Computo de la media.
        int N = datos.length;
        double med = 0;
        for (int k = 0; k < N; k++) {
            
            med += datos[k];
        }
        med = med / N;
        return med;
    }
    
    // Metodo para el calculo de la varianza de un vector de muestras.
    
    private static double varianza(double[] datos) {
        // Computo de la media.
        int N = datos.length;
        double med = media(datos);
        // Computo de la varianza.
        double varianza = 0;
        for (int k = 0; k < N; k++) {
            varianza += Math.pow(datos[k] - med, 2);
        }
        varianza = varianza / (N - 1);
        return varianza;
    }

    
	// Metodo para la normalizacion de un vector de muestras.
    
    private static double[] normaliza(double[] datos) {
       
    	double maximo = 0;
        for (int k = 0; k < datos.length; k++) {
            if (Math.abs(datos[k]) > maximo) {
                maximo = Math.abs(datos[k]);
            }
        }
        for (int k = 0; k < datos.length; k++) {
            datos[k] = datos[k] / maximo;
        }
        return datos;
    }

    
    
    // Metodo para enventanar Hamming un vector de muestras.
    
    private static double[] aplicaHamming(double[] datos) {
        double A0 = 0.53836;
        double A1 = 0.46164;
        int Nbf = datos.length;
        for (int k = 0; k < Nbf; k++) {
            datos[k] = datos[k] * (A0 - A1 * Math.cos(2 * Math.PI * k / (Nbf - 1)));
        }
        return datos;
    }
	

	

    // Función que devuelve un objeto de la clase Maximo,que contiene:
	// valor máximo y posicion en la trama que se pasa como parametro.
	// Entradas:
	// - x = trama o array a analizar
	// - ini = comienzo de la trama
	// - fin = fin de la trama
	// Salida:
	// - Maximo: objeto de la clase Maximo que contiene (valor, posicion)
	// del maximo de la trama
	public Maximo max(double[] x, int ini, int fin) {

		Maximo miMaximo;
		miMaximo = new Maximo();

		for (int i = ini; i < fin; i++) {
			if (Math.abs(x[i]) >= miMaximo.valor) {
				miMaximo.valor = Math.abs(x[i]);
				miMaximo.pos = i;
			}

		}

		return miMaximo;

	}
	
	// Definicion de la clase del objeto Maximo	
	class Maximo {
		int pos;// posicion
		double valor;
	}


	public void onClick(View v) {
		if (started) {

			started = false;
			startStopButton.setText("ON");
            recordTask.cancel(true);
            validos[1]=0;



        } else {
			started = true;
			startStopButton.setText("OFF");
			recordTask = new RecordAudio();
			recordTask.execute();

		}
	}
 //funcion que toma las medidas de la pantalla y la relacion de dimensiones entre el alto yb el ancho para manejar el tamaño del texto
	public int [] screenDimension (){
		int [] pantalla = new int[3];
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		pantalla[0] = (int) Math.round((double)size.x); // [0] eje X (ancho)
		pantalla[1] = (int) Math.round((double)size.y *0.6) ; // [1] eje y (alto)
		pantalla[2] = (int) Math.round((double)pantalla[0]/(double)pantalla[1]); //adptativo: [2] relacion de dimensiones.
		return pantalla;
	}

}