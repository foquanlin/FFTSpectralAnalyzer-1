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
	

	/////////////////////////////////////
	
	//Ojeto de tipo WakeLock que permite mantener despierta la aplicacion
	protected PowerManager.WakeLock wakelock;
	

	
	RecordAudio recordTask; // proceso de grabacion y analisis
	AudioRecord audioRecord; // objeto de la clase AudioReord que permite captar el sonido
	
	Button startStopButton; // boton de arranquue y pausa
	boolean started = false; // condicion del boton
	
	
	
	// Configuracion del canal de audio
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	static double RATE = 8000; // frecuencia o tasa de muestreo
	
	
	int bufferSize = 0;  // tamaño del buffer segun la configuracion de audio
	int bufferReadResult = 0; // tamaño de la lectura
	int blockSize_buffer = 1024; // valor por defecto para el bloque de lectura de buffer
	
	
	
	// Objeto de la clase que determina la FFT de un vector de muestras
	private RealDoubleFFT transformer;
	int blockSize_fft = 2048; // tamaño de la transformada de Fourier
	
	
	// Frecuencias del rango de estudio asociadas al instrumento
	static double MIN_FREQUENCY = 50; // HZ
	static double MAX_FREQUENCY = 3000; // HZ
	
		
	// Valores pordefecto para el estudio de los armonicos
  
	double UMBRAL = 100; // umbral de amplitud valido para tener en cuenta 
						 // los armonicos, depende del tamaño de la FFT
	
	int LONGTRAMA = 20; // tamaño de la ventana de estudio de los armonicos 
						// tambien depende del tamaño de la FFT
	
	int NUM_ARMONICOS = 6; // numero de armonicos a tener en cuenta 
	
	int REL_AMP = 8; // relacion de amplitudes que han de tener los dos primeros armonicos para hallar la nota
	int REL_FREC = 4; // relacion en frecuencia que han de tener los dos primeros armonicos para hallar la nota
	
	double[] aux3;{ // declaracion de vector auxiliar para el estudio de la trama
	aux3 = new double[LONGTRAMA];} // sera el array que contenga la amplitud de los armonicos
	
	double [] validos = new double[NUM_ARMONICOS] ; // vector que tendra solo los armonicos de interes
	double [] amplitudes = new double[NUM_ARMONICOS] ; // vector con las amplitudes de estos armonicos de interes
	
	double freq_asociada = 0; // valor de la frecuencia obtenida como fundamental tras el estudio de los armonicos
	

	

	

	

	

	
	// Elementos para la representacion en pantalla





    int alturaGrafica = 600; // tamaño vertical de la grafica

	int blockSize_grafica = 1500; // tamaño horizontal de la grafica

	// Calculamos el cociente de la Relacion de Aspecto que usaremos para ubicar
	// todo aquello cuya posicion varie en funcion de un valor determinado
	int factor = (int) Math.round((double)blockSize_grafica/(double)alturaGrafica); //adptativo

	// Tamaños de texto para los diferentes mensajes y resultados
	int TAM_TEXT = 40;
	int TAM_TEXT1 = 10*factor;
	int TAM_TEXT2 = 5*factor;
	int TAM_TEXT3 = 7*factor;

	
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

		int factor = (int) Math.round((double)blockSize_grafica/(double)alturaGrafica); //adptativo

		// Tamaños de texto para los diferentes mensajes y resultados
		int TAM_TEXT = 40;
		int TAM_TEXT1 = 10*factor;
		int TAM_TEXT2 = 5*factor;
		int TAM_TEXT3 = 7*factor;



		 // imagen para la representacion del espectro
		imageView = (ImageView) this.findViewById(R.id.ImageView01);


		bitmap = Bitmap.createBitmap(blockSize_grafica, alturaGrafica,
				Bitmap.Config.ARGB_8888);


		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		imageView.setImageBitmap(bitmap);
		
		// imagen para dibujar las bandas de frecuencia


		imageView2 = (ImageView) this.findViewById(R.id.ImageView02);
		bitmap2 = Bitmap.createBitmap((int) blockSize_grafica, TAM_TEXT1,
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
			
			// Arrays con las muestras de audio en tiempo y frecuencia en formato double
			double[] trama, trama_espectro;	
			trama = new double[blockSize_fft];
			
			// inicializamos el vector que contendra la FFT de
			transformer = new RealDoubleFFT(blockSize_fft);

			
			for (int i = 0; i < bufferReadResult; i++) {
									
				trama[i * 2] = (double) toTransform[0][i];	
				trama[i * 2 + 1] = 0; // aumentaremos la resolucion en frecuencia de la transformada interpolando ceros
				
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
			
						
			DibujaEspectro(trama_espectro); // representa graficamente el espectro de la señal				
			
			// Dibuja una linea roja que representa el promedio del espectro
			//canvas5.drawLine(0, alturaGrafica -(float)promedio(trama_espectro)*alturaGrafica, 
			//		blockSize_grafica,alturaGrafica -(float)promedio(trama_espectro)*alturaGrafica, paint5);

			
			// Dibuja linea cyan con el umbral seleccionado por el usuario
			canvas6.drawLine(0, alturaGrafica - altura_umbral, blockSize_grafica,alturaGrafica -altura_umbral, paint6);
			
			
			EscribirArmonicos();
		
			
			
		}
	}





    public void EscribirArmonicos(){
		
    	
		paint4.setAntiAlias(true);
		paint4.setFilterBitmap(true);			
		paint4.setTextSize(TAM_TEXT2);
				
		
		
		


    	
    }

    ///////////////////////////////////////////////////////////////////////////////
	//DIBUJA EL EJE DE FRECUENCIAS/////////////////////////////////////////////////
    public void DibujaEjeFrecuencias(){
    	
		
		canvas2.drawColor(Color.BLACK);
		paint2.setAntiAlias(true);
		paint2.setFilterBitmap(true);
		
		// Valores que se mostrara en el eje X
		int[]bandas ={220,440,880,1320,1760,2350};
		paint2.setStrokeWidth(5);
		canvas2.drawLine(0,0,blockSize_grafica,0,paint2);
		
		paint2.setTextSize(TAM_TEXT3);
		canvas2.drawText(String.valueOf(bandas[0]),bandas[0]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[1]),bandas[1]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[2]),bandas[2]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[3]),bandas[3]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[4]),bandas[4]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText(String.valueOf(bandas[5]),bandas[5]/factor-(TAM_TEXT3)/2,TAM_TEXT3,paint2);
		canvas2.drawText("Hz",blockSize_grafica - TAM_TEXT1,TAM_TEXT3,paint2);
				
		imageView2.invalidate();
		
		
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////
	//DIBUJA EL ESPECTRO///////////////////////////////////////////////////////////
    public void DibujaEspectro(double[] trama_espectro){
    	
    	// Claculo del la relacion Señal a Ruido (dB)
		// Resulta del cociente entre el valor maximo del espectro entre el pormedio
		// Lo ideal es que la SNR valga infinio, lo que significa que no hay ruido
		//double snr2 = 10*Math.log10(max(trama_espectro,0,trama_espectro.length).valor/promedio(trama_espectro));
    	
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < trama_espectro.length; i++) {
			int x = i;
			int downy = (int) (alturaGrafica - (trama_espectro[i]*alturaGrafica));
			int upy = alturaGrafica;
			
			canvas.drawLine(x, downy, x, upy, paint);
			
		}
		
		//paint3.setAntiAlias(true);
		//paint3.setFilterBitmap(true);
		//paint3.setTextSize(TAM_TEXT2);
		//canvas3.drawText(" SNR: " + df1.format(snr2) + " dB", blockSize_grafica-alTuraGrafica, TAM_TEXT3, paint3);
		
		imageView.invalidate();
		
    }
    




    
    
    // Algoritmo que recibe como parametro de entrada una trama de frecuencias y determina cual es el 
    // principal armonico, es decir el de mayor amplitud
	public double devuelvePitch(double[] data) {

		
		// indice o poscion en el que empezaremos a leer en la trama del espectro
		// que se corresponde con la frecuencia mínima del rango de deteccion
		final int min_frequency_fft = (int) Math.round(MIN_FREQUENCY
				* blockSize_buffer / RATE); 
		// indice o poscion en el que acabaremos de leer en la trama del espectro
		// que se corresponde con la frecuencia máxima del rango de deteccion
		final int max_frequency_fft = (int) Math.round(MAX_FREQUENCY
				* blockSize_buffer / RATE);
		double best_frequency = min_frequency_fft; // inicializamos la frecuencia candidata en el minimo
		double best_amplitude = 0; // inicializamos a 0 la amplitud que al final sera la maxima de la trama 
		
		// recorremos la trama 
		for (int i = min_frequency_fft; i <= max_frequency_fft; i++) {
			
			// calcula la frecuncia actual restaurando el valor del indice o posicon
			final double current_frequency = i * 1.0 * RATE
					/(blockSize_buffer);
			
			
			//final double normalized_amplitude = Math.abs(data[i]);
			
			// calcula la amplitud actual
			final double current_amplitude = Math.pow(data[i * 2], 2)
					+ Math.pow(data[i * 2 + 1], 2);
			
			// normaliza la amplitud actual
			final double normalized_amplitude = current_amplitude
					* Math.pow(MIN_FREQUENCY * MAX_FREQUENCY, 0.5)
					/ current_frequency;
			
			// si es mayor que la anterior es candidata a ser la maxima
			if (normalized_amplitude > best_amplitude) {
				best_frequency = current_frequency;
				best_amplitude = normalized_amplitude;
			}
		}
		return best_frequency;

	} 

	
	// Algoritmo que recibe como parametro de entrada una trama de frecuencias y determina cuales son los 
    // principales armonicos que componen la señal en funcion del criterio de comparacion con un UMBRAL
	public double[] devuelveArmonicos(double[] data) {
		
		int r = 0; // indice para el recorrido del vector con los armonicos
		
		int umbral = (int)UMBRAL; // condicion necesaria para considerarse armonico
		int longtrama = LONGTRAMA; // longitud de la trama para el estudio de los armonicos
		
		
		// indice o poscion en el que empezaremos a leer en la trama del espectro
		// que se corresponde con la frecuencia mínima del rango de deteccion
		final int min_frequency_fft = (int) Math.round(MIN_FREQUENCY
				* blockSize_buffer / RATE);
		
		// indice o poscion en el que acabaremos de leer en la trama del espectro
		// que se corresponde con la frecuencia máxima del rango de deteccion
		final int max_frequency_fft = (int) Math.round(MAX_FREQUENCY
				* blockSize_buffer / RATE);
		double best_frequency = min_frequency_fft; // inicializamos la frecuencia candidata en el minimo
				
		double best_amplitude = 0; // inicializamos la amplitud a comparar con el umbral a 0
		
		double[] aux2; // declaracion de vector auxiliar para el estudio de la trama
		aux2 = new double[longtrama]; // sera el array que contenga los armonicos
		
		
		// bucle que recorre hasta la posicion del maximo del rango [MIN_FREQUENCY,MAX_FREQUENCY]
		for (int i = min_frequency_fft; i< max_frequency_fft; i = i + longtrama) {				
        	
			best_amplitude = 0; // restauramos el valor de la amplitud maxima una vez leida la trama
			
			// bucle que recorre la trama
				for (int j = 0; j < longtrama; j++) {
		
					
					final double current_frequency = (i+j) * 1.0 * RATE
							/(blockSize_buffer);
					
					//final double normalized_amplitude = Math.abs(data[i+j]);
					
					final double current_amplitude = Math.pow(data[(i+j) * 2], 2)
							+ Math.pow(data[(i+j) * 2 + 1], 2);
					final double normalized_amplitude = current_amplitude
							* Math.pow(MIN_FREQUENCY * MAX_FREQUENCY, 0.5)
							/ current_frequency;
		
					if (normalized_amplitude > best_amplitude) {
						best_frequency = current_frequency;
						best_amplitude = normalized_amplitude;
					}
				}
				
				if(best_amplitude>umbral){
					// almacena en aux2 la posicion frecuencia 
					// que cumple el requisito 'umbral'
					// y en aux3 la amplitud correspondiente
					
					aux3[r] = best_amplitude;
					aux2[r] = best_frequency;
					r = r + 1;
					
					
				}
				
		}
		return aux2; // devuelve el vector con las frecuencias de los armonicos

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

}