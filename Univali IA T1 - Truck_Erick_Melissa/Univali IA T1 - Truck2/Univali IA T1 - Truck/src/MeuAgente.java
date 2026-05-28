import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;


public class MeuAgente extends Agente {
	
	Color color;
	
	double vel = 100;
	double  ang  = 0;
	
	double angVolante = 0;
	int volanteinfvalue = 0;
	int volanteRotationSpeed = 120;
	
	
	double oldx = 0;
	double oldy = 0;
	
	int timeria = 0;
	
	boolean colidiu = false;
	
	boolean start = false;
	boolean startAnterior = false;
	CaminhaoFuzzyController controladorFuzzy = new CaminhaoFuzzyController();
	double ultimoXProgresso = 0;
	double ultimoYProgresso = 0;
	int framesParado = 0;
	
	ArrayList<Rectangle> listaDeObstaculos = null;
	
	Polygon poly = new Polygon();
	Polygon poly2 = new Polygon();
	
	public MeuAgente(int x,int y, Color color,ArrayList<Rectangle> listaDeObstaculos) {
		// TODO Auto-generated constructor stub
		X = x;
		Y = y;
		
		this.color = color;
		this.listaDeObstaculos = listaDeObstaculos;
		
		poly.addPoint(-20, -10);
		poly.addPoint(25, -10);
		poly.addPoint(25, 10);
		poly.addPoint(-20, 10);
		
	}
	
	@Override
	public void SimulaSe(int DiffTime) {
		// TODO Auto-generated method stub
		timeria+=DiffTime;
		
		oldx = X;
		oldy = Y;
		
		if(start && !startAnterior){
			timeria = 0;
			controladorFuzzy.reset();
			ultimoXProgresso = X;
			ultimoYProgresso = Y;
			framesParado = 0;
		}
		startAnterior = start;
		
		angVolante += volanteinfvalue*volanteRotationSpeed*DiffTime/1000.0;
		
		if(angVolante > 90){
			angVolante = 90;
		}
		
		if(angVolante < -90){
			angVolante = -90;
		}
		
		if(start && timeria>60){
			calculaIA(DiffTime);
			timeria = 0;
		}
		
		if(start){
			ang += vel/100.0*((angVolante*Math.PI/2)/90.0f)*DiffTime/1000.0;
			
			X+=Math.cos(ang)*vel*DiffTime/1000.0;
			Y+=Math.sin(ang)*vel*DiffTime/1000.0;
		}
		
		poly2 = new Polygon(poly.xpoints,poly.ypoints,poly.npoints);

		for(int i = 0; i < poly2.npoints;i++){
			double x = poly2.xpoints[i];
			double y = poly2.ypoints[i];
			
			double x2 = x*Math.cos(ang) - y*Math.sin(ang);
			double y2 = y*Math.cos(ang) + x*Math.sin(ang);
			
			poly2.xpoints[i] = (int)x2;
			poly2.ypoints[i] = (int)y2;
		}		
		
		poly2.translate((int)X, (int)Y);
		
		// O que fizemos para resolver colisão
		colidiu = false;
		for(int i = 0; i < listaDeObstaculos.size();i++){
			if(poly2.intersects(listaDeObstaculos.get(i))){
				X = oldx;
				Y = oldy;
				colidiu = true;
				break;
			}
		}

		verificaTravamento();

	}

	@Override
	public void DesenhaSe(Graphics2D dbg, int XMundo, int YMundo) {
		// TODO Auto-generated method stub
		dbg.setColor(Color.red);
		dbg.draw(poly2);
		
		dbg.setColor(color);
		
		AffineTransform trans = dbg.getTransform();
		
		dbg.translate(X, Y);
		dbg.rotate(ang);
		
		dbg.drawRect(-20, -10, 40, 20);
		
		dbg.drawRect(20, -5, 5,10);
		
		dbg.setTransform(trans);
	
	}
	
	
	public void rodaVolante(int v){
		if(v > 0){
			volanteinfvalue = 1;
		}else if(v < 0){
			volanteinfvalue = -1;
		}else{
			volanteinfvalue = 0;
		}
	}
	
	public void acelera(int v){
		if(v > 0){
			vel = 100;
		}else if(v < 0){
			vel = -100;
		}else{
			vel = 0;
		}
	}

	private void setVolante(double valor){
		angVolante = Math.max(-90, Math.min(90, valor));
		volanteinfvalue = 0;
	}

	private void setVelocidade(double valor){
		vel = Math.max(-100, Math.min(100, valor));
	}

	//Caso ele esteja parado em qualquer lugar que não seja onde deve esta estacionado ele vai ativar a funcao de
	//verificaTravamento para reiniciar o objetivo
	private boolean estaEstacionado(){
		double erroX = 400 - X;
		double erroY = 20 - Y;
		double erroAng = -Math.PI/2 - ang;
		while(erroAng > Math.PI){
			erroAng -= 2*Math.PI;
		}
		while(erroAng < -Math.PI){
			erroAng += 2*Math.PI;
		}
		return Math.abs(erroX) < 8 && Math.abs(erroY) < 5 && Math.abs(Math.toDegrees(erroAng)) < 7;
	}

	private void verificaTravamento(){
		if(!start || estaEstacionado()){
			framesParado = 0;
			ultimoXProgresso = X;
			ultimoYProgresso = Y;
			return;
		}
		
		double movimento = Math.hypot(X - ultimoXProgresso, Y - ultimoYProgresso);
		if(movimento < 0.25){
			framesParado++;
		}else{
			framesParado = 0;
			ultimoXProgresso = X;
			ultimoYProgresso = Y;
		}
		
		if(framesParado > 120){
			controladorFuzzy.reiniciarObjetivo();
			framesParado = 0;
			ultimoXProgresso = X;
			ultimoYProgresso = Y;
		}
	}

	public void calculaIA(int DiffTime){
		CaminhaoFuzzyController.Controle controle = controladorFuzzy.calcular(X, Y, ang, colidiu, DiffTime);
		setVolante(controle.volante);
		setVelocidade(controle.velocidade);
		colidiu = false;
	}
	
}
