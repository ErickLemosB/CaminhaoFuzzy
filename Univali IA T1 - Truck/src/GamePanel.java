import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.awt.image.*;
import javax.imageio.ImageIO;


public class GamePanel extends Canvas implements Runnable
{
private static final int PWIDTH = 800;
private static final int PHEIGHT = 500;
private Thread animator;
private boolean running = false;
private boolean gameOver = false; 


int FPS,SFPS;
int fpscount;

public static Random rnd = new Random();

BufferedImage imagemcharsets;

boolean LEFT, RIGHT,UP,DOWN;

public static int mousex,mousey; 

public static MeuAgente myTruck;

//public static ArrayList<Agente> listadeagentes = new ArrayList<Agente>();

public static ArrayList<Rectangle> listaDeObstaculos = new ArrayList<Rectangle>();

public int clickState = 0;

public GamePanel()
{

	setBackground(Color.white);
	setPreferredSize( new Dimension(PWIDTH, PHEIGHT));

	// create game components
	setFocusable(true);

	requestFocus(); // JPanel now receives key events	
	
	
	// Adiciona um Key Listner
	addKeyListener( new KeyAdapter() {
		public void keyPressed(KeyEvent e)
			{ 
				int keyCode = e.getKeyCode();
				
				if(keyCode == KeyEvent.VK_LEFT){
					LEFT = true;
				}
				if(keyCode == KeyEvent.VK_RIGHT){
					RIGHT = true;
				}
				if(keyCode == KeyEvent.VK_UP){
					UP = true;
				}
				if(keyCode == KeyEvent.VK_DOWN){
					DOWN = true;
				}	
				if(keyCode == KeyEvent.VK_SPACE){
					myTruck.start=!myTruck.start;
				}	
			}
		@Override
			public void keyReleased(KeyEvent e ) {
				int keyCode = e.getKeyCode();
				
				if(keyCode == KeyEvent.VK_LEFT){
					LEFT = false;
				}
				if(keyCode == KeyEvent.VK_RIGHT){
					RIGHT = false;
				}
				if(keyCode == KeyEvent.VK_UP){
					UP = false;
				}
				if(keyCode == KeyEvent.VK_DOWN){
					DOWN = false;
				}
			}
	});
	
	addMouseMotionListener(new MouseMotionListener() {
		
		@Override
		public void mouseMoved(MouseEvent e) {
			// TODO Auto-generated method stub
			mousex = e.getX(); 
			mousey = e.getY();
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			// TODO Auto-generated method stub
		}
	});
	
	
	addMouseListener(new MouseListener() {
		
		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			if(clickState==0){
				myTruck.X = arg0.getX();
				myTruck.Y = arg0.getY();
				clickState = 1;
			}else if(clickState==1){
				double difx = arg0.getX() - myTruck.X;
				double dify = arg0.getY() - myTruck.Y;
				
				myTruck.ang = Math.atan2(dify, difx);
				clickState = 0;
			}
		}
		
		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	});
	
	try {
		imagemcharsets = ImageIO.read( getClass().getResource("Chara1.png") );
	}
	catch(IOException e) {
		System.out.println("Load Image error:");
	}
	
	myTruck = new MeuAgente(300, 250, Color.blue,listaDeObstaculos);

	mousex = mousey = 0;
	
	listaDeObstaculos.add(new Rectangle(0,0,385,40));
	listaDeObstaculos.add(new Rectangle(415,0,385,40));
	listaDeObstaculos.add(new Rectangle(0,490,800,10));
	listaDeObstaculos.add(new Rectangle(0,0,10,500));
	listaDeObstaculos.add(new Rectangle(790,0,10,500));
} // end of GamePanel()

public void startGame()
// initialise and start the thread
{
	if (animator == null || !running) {
		animator = new Thread(this);
		animator.start();
	}
} // end of startGame()

public void stopGame()
// called by the user to stop execution
{ running = false; }


public void run()
/* Repeatedly update, render, sleep */
{
	running = true;
	
	long DifTime,TempoAnterior;
	
	int segundo = 0;
	DifTime = 0;
	TempoAnterior = System.currentTimeMillis();
	
	this.createBufferStrategy(2);
	BufferStrategy strategy = this.getBufferStrategy();
	
	while(running) {
	
		gameUpdate(DifTime); // game state is updated
		Graphics g = strategy.getDrawGraphics();
		gameRender((Graphics2D)g); // render to a buffer
		strategy.show();
	
		try {
			Thread.sleep(0); // sleep a bit
		}	
		catch(InterruptedException ex){}
		
		DifTime = System.currentTimeMillis() - TempoAnterior;
		TempoAnterior = System.currentTimeMillis();
		
		if(segundo!=((int)(TempoAnterior/1000))){
			FPS = SFPS;
			SFPS = 1;
			segundo = ((int)(TempoAnterior/1000));
		}else{
			SFPS++;
		}
	
	}
System.exit(0); // so enclosing JFrame/JApplet exits
} // end of run()

int timerfps = 0;
private void gameUpdate(long DiffTime)
{ 
	if(!myTruck.start){
		if(UP){
			myTruck.acelera(+1);
		}else if(DOWN){
			myTruck.acelera(-1);
		}else{
			myTruck.acelera(0);
		}
		
		if(LEFT){
			myTruck.rodaVolante(-1);
		}else if(RIGHT){
			myTruck.rodaVolante(+1);
		}else{
			myTruck.rodaVolante(0);
		}
	}
	
	myTruck.SimulaSe((int)DiffTime);
}

private void gameRender(Graphics2D dbg)
// draw the current frame to an image buffer
{
	// clear the background
	dbg.setColor(Color.white);
	dbg.fillRect (0, 0, PWIDTH, PHEIGHT);
	
	dbg.setColor(Color.black);
	for(int i = 0; i < listaDeObstaculos.size();i++){
		dbg.draw(listaDeObstaculos.get(i));
	}
	
	myTruck.DesenhaSe(dbg, 0, 0);

	dbg.setColor(Color.black);
	dbg.drawOval(700, 10, 100, 100);
	dbg.drawOval(705, 15, 90, 90);
	dbg.setColor(Color.green);
	int cx = (int)((Math.cos(Math.toRadians(myTruck.angVolante-90))*47)+750);
	int cy = (int)((Math.sin(Math.toRadians(myTruck.angVolante-90))*47)+60);
	dbg.fillOval(cx-3, cy-3, 6, 6);
	dbg.setColor(Color.black);
	dbg.drawLine(700, 60, 705, 60);
	dbg.drawLine(795, 60, 800, 60);
	dbg.drawLine(750, 10, 750, 15);
	
	
	
	dbg.setColor(Color.BLUE);	
	dbg.drawString("FPS: "+FPS+" Vel "+myTruck.vel+" "+myTruck.ang, 10, 10);	
	if(!myTruck.start){
		dbg.drawString("FREIO DE MAO", 300, 300);		
	}
	
	
	//System.out.println("left "+LEFT);
		
}

}

