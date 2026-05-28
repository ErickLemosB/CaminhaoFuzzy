public class CaminhaoFuzzyController {

	public static class Controle {
		public double volante;
		public double velocidade;

		public Controle(double volante, double velocidade) {
			this.volante = volante;
			this.velocidade = velocidade;
		}
	}

	// Usado apenas para recuperação de colisão — não é parte da lógica de estacionamento
	private int tempoRecuando = 0;

	public void reset() {
		tempoRecuando = 0;
	}

	public void reiniciarObjetivo() {
		tempoRecuando = 60;
	}

	// -------------------------------------------------------------------------
	// Funções auxiliares
	// -------------------------------------------------------------------------

	private double limita(double valor, double min, double max) {
		return Math.max(min, Math.min(max, valor));
	}

	private double normalizaAngulo(double valor) {
		while (valor > Math.PI)  valor -= 2 * Math.PI;
		while (valor < -Math.PI) valor += 2 * Math.PI;
		return valor;
	}

	// Pertinência triangular: sobe de a até b, desce de b até c
	private double triangular(double x, double a, double b, double c) {
		if (a == b && x <= b) return 1;
		if (b == c && x >= b) return 1;
		if (x <= a || x >= c) return 0;
		if (x == b) return 1;
		if (x < b) return (x - a) / (b - a);
		return (c - x) / (c - b);
	}

	// Pertinência trapezoidal: sobe de a→b, plano de b→c, desce de c→d
	private double trapezoidal(double x, double a, double b, double c, double d) {
		if (x <= a || x >= d) return 0;
		if (x >= b && x <= c) return 1;
		if (x < b) return (x - a) / (b - a);
		return (d - x) / (d - c);
	}

	// -------------------------------------------------------------------------
	// Módulos fuzzy de controle (reutilizados pelos 3 modos de comportamento)
	// -------------------------------------------------------------------------

	// Calcula quanto girar o volante dado um erro angular em graus
	private double fuzzyVolante(double erroGraus) {
		double muitoEsq = triangular(erroGraus, -180, -90, -25);
		double poucoEsq = triangular(erroGraus,  -45, -18,   0);
		double centro   = triangular(erroGraus,   -8,   0,   8);
		double poucoDir = triangular(erroGraus,    0,  18,  45);
		double muitoDir = triangular(erroGraus,   25,  90, 180);

		double num = muitoEsq * (-90) + poucoEsq * (-38) + centro * 0 + poucoDir * 38 + muitoDir * 90;
		double den = muitoEsq + poucoEsq + centro + poucoDir + muitoDir;

		if (den == 0) return erroGraus < 0 ? -90 : 90;
		return limita(num / den, -90, 90);
	}

	// Calcula velocidade base dado distância ao alvo e desalinhamento angular
	private double fuzzyVelocidade(double distancia, double erroGrausAbs) {
		double perto = triangular(distancia,   0,   0,  45);
		double medio = triangular(distancia,  25,  90, 170);
		double longe = triangular(distancia, 120, 280, 520);

		double alinhado    = triangular(erroGrausAbs,   0,   0,  16);
		double medioAng    = triangular(erroGrausAbs,   8,  35,  75);
		double desalinhado = triangular(erroGrausAbs,  55, 130, 180);

		double devagar = Math.max(perto, desalinhado);
		double normal  = Math.max(Math.min(medio, alinhado), Math.min(longe, medioAng));
		double rapido  = Math.min(longe, alinhado);

		double num = devagar * 35 + normal * 65 + rapido * 90;
		double den = devagar + normal + rapido;

		if (den == 0) return 35;
		return limita(num / den, 25, 95);
	}

	// -------------------------------------------------------------------------
	// Controlador principal — 100% fuzzy, sem comutação de estados discretos
	// -------------------------------------------------------------------------
	//
	// Ideia central: volante/velocidade saem de regras fuzzy contínuas que
	// equilibram aproximar o pré-ponto, alinhar e entrar na vaga.
	// -------------------------------------------------------------------------
	public Controle calcular(double x, double y, double ang, boolean colidiu, int diffTime) {
		final double vagaX  = 400;
		final double vagaY  =  20;
		final double preX   = 400;
		final double preY   = 165;
		final double angFinal = -Math.PI / 2;   // apontando para cima (−90°)

		// Recuperação de colisão: recua por um tempo, depois retoma
		if (colidiu) tempoRecuando = 60;
		if (tempoRecuando > 0) {
			tempoRecuando -= diffTime;
			return new Controle(x < vagaX ? 55 : -55, -45);
		}

		// Erros e distâncias fundamentais
		double erroX            = vagaX - x;
		double erroAngFinal     = normalizaAngulo(angFinal - ang);
		double erroAngFinalGraus = Math.toDegrees(erroAngFinal);
		double distVaga         = Math.hypot(vagaX - x, vagaY - y);
		double distPre          = Math.hypot(preX  - x, preY  - y);

		// Condição de parada: estacionado com precisão suficiente
		if (Math.abs(erroX) < 8 && Math.abs(vagaY - y) < 5 && Math.abs(erroAngFinalGraus) < 7) {
			return new Controle(0, 0);
		}

		// =====================================================================
		// BLOCO 1 — Funções de pertinência das entradas fuzzy
		// =====================================================================

		// Quão bem alinhado está o ângulo em relação a −90°
		// (1 = perfeitamente alinhado, 0 = completamente errado)
		double mAlinhado = triangular(Math.abs(erroAngFinalGraus), 0, 0, 18);

		// Quão bem centralizado está em X sobre a vaga
		// (1 = centrado, 0 = desviado)
		double mCentrado = triangular(Math.abs(erroX), 0, 0, 35);

		// Progresso ao longo do corredor de entrada: 0 = no nível do pré-ponto,
		// 1 = na posição da vaga. Garante que, uma vez iniciada a entrada,
		// o modo A (aproximar) não seja reativado pelo aumento de distPre.
		double progressoEntrada = limita((preY - y) / (preY - vagaY), 0.0, 1.0);

		// "Já entrou no corredor" — permanece 1 depois que o caminhão cruzou o
		// pré-ponto em direção à vaga (protege contra rever para modo A)
		double mJaEntrou = trapezoidal(progressoEntrada, 0.03, 0.12, 1.0, 1.1);

		// Distâncias ao pré-ponto (aproximação) — sem estados, apenas pertinência
		double mPertoPre = triangular(distPre, 0, 0, 120);
		double mLongePre = triangular(distPre, 80, 240, 520);

		// =====================================================================
		// BLOCO 2 — Força das regras fuzzy
		// =====================================================================
		//
		// Regra A: longe do pré-ponto -> aproximar do pré-ponto
		// Regra B: perto do pré-ponto e desalinhado -> alinhar
		// Regra C: alinhado/centrado ou já entrou -> entrar na vaga
		double rAproximar = mLongePre * (1.0 - mJaEntrou);
		double rAlinhar   = mPertoPre * (1.0 - mAlinhado * mCentrado) * (1.0 - mJaEntrou);
		double rEntrar    = Math.max(mJaEntrou, mPertoPre * mAlinhado * mCentrado);

		double totalW = rAproximar + rAlinhar + rEntrar;
		if (totalW < 1e-9) totalW = 1e-9;

		// =====================================================================
		// BLOCO 3 — Cálculo do volante por modo
		// =====================================================================

		// Regra A: apontar e seguir em direção ao pré-ponto
		double angPre      = Math.atan2(preY - y, preX - x);
		double erroPre     = Math.toDegrees(normalizaAngulo(angPre - ang));
		double steerAproximar = fuzzyVolante(erroPre);

		// Regra B: corrigir o ângulo final (−90°) e centralizar em x=vagaX
		double steerAlinhar = fuzzyVolante(erroAngFinalGraus) + limita(erroX * 0.8, -32, 32);

		// Regra C: guiar diretamente para a entrada da vaga, corrigindo x
		double angEntrada  = Math.atan2(vagaY - y, vagaX - x);
		double erroEntrada = Math.toDegrees(normalizaAngulo(angEntrada - ang));
		double steerEntrar = fuzzyVolante(erroEntrada) * 0.65 + erroX * 1.15;

		// Defuzzificação do volante: média ponderada pelas forças das regras
		double steer = (rAproximar * steerAproximar + rAlinhar * steerAlinhar + rEntrar * steerEntrar) / totalW;

		// =====================================================================
		// BLOCO 4 — Cálculo da velocidade por modo
		// =====================================================================

		// Regra A: velocidade fuzzy baseada em distância e desalinhamento ao pré-ponto
		double velAproximar = fuzzyVelocidade(distPre, Math.abs(erroPre));

		// Regra B: devagar enquanto faz a manobra de alinhamento
		double velAlinhar = 30.0;

		// Regra C: velocidade de entrada com suavização ao chegar no alvo.
		// Se o caminhão ultrapassou a vaga (y < vagaY), reduz progressivamente
		// até reverter, usando pertinência fuzzy de "sobrepassou".
		double mSobrepassou  = limita((vagaY - y) / 8.0, 0.0, 1.0);
		double velEnterBase  = limita(fuzzyVelocidade(distVaga, Math.abs(erroEntrada)), 14, 42);
		double velEntrar     = velEnterBase * (1.0 - mSobrepassou) - 14.0 * mSobrepassou;

		// Defuzzificação da velocidade: média ponderada pelas forças das regras
		double vel = (rAproximar * velAproximar + rAlinhar * velAlinhar + rEntrar * velEntrar) / totalW;

		return new Controle(limita(steer, -90, 90), vel);
	}
}
