package es.resultados.fft;

public class Lagrange {

        private int y[];
        private int x[];
        private double a[];

        public Lagrange(int y[], int x[]) {
            this.x = x;
            this.y = y;
            a=new double[x.length];
        }

        private void getPolinomios_a() {

            double mult;

            for(int i=0;i<y.length;i++){
                mult = 1;

                for(int j=0;j<x.length;j++){

                    if(i==j)continue;
                    mult=(x[i]-x[j])*mult;
                }
                a[i]=y[i]/mult;
                ///System.out.print("a"+"["+i+"]="+a[i]+"  ");


            }
        }

        public double getResultadoP(int p){
            getPolinomios_a();

            int mult=1;
            double valores[]=new double[a.length];
            //System.out.println("");
            for(int i=0;i<a.length;i++){
                mult=1;
                for(int j=0;j<a.length;j++){

                    if(i==j)continue;
                    mult=(p-x[j])*mult;

                }
                valores[i]=a[i]*mult;
                //System.out.print(valores[i]);
                //if(i!=a.length-1)System.out.print(" + ");

            }

            double resultado=0;
            for(int k=0;k<valores.length;k++){

                resultado=resultado+valores[k];
            }

            return resultado;

        }

    }
