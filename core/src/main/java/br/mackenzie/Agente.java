package br.mackenzie;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Agente autônomo usado nos comportamentos de steering da atividade.
 * Estrutura baseada nos exemplos dos slides do professor.
 */
public class Agente {

    public enum Estado {
        EXPLORANDO,
        PERSEGUINDO
    }

    public final Vector2 posicao = new Vector2();
    public final Vector2 velocidade = new Vector2();
    public final Vector2 ultimaForca = new Vector2();

    public float maxVelocidade = 3.2f;
    public float maxForca = 8f;
    public float massa = 1f;
    public float raio = 0.20f;

    public Estado estado = Estado.EXPLORANDO;

    private float anguloWander = 0f;

    private static final float DIST_CIRCULO = 1.2f;
    private static final float RAIO_CIRCULO = 0.8f;
    private static final float VARIACAO = 0.35f;

    public Agente(float x, float y) {
        posicao.set(x, y);
        velocidade.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f));
        if (velocidade.isZero()) {
            velocidade.set(1f, 0f);
        }
        velocidade.nor().scl(maxVelocidade * 0.5f);
    }

    // SEEK: procura o alvo usando velocidade desejada.
    public Vector2 seek(Vector2 alvo) {
        Vector2 desejada = new Vector2(alvo)
            .sub(posicao)
            .nor()
            .scl(maxVelocidade);

        return desejada.sub(velocidade);
    }

    // FLEE: inverso do seek.
    public Vector2 flee(Vector2 ameaca) {
        Vector2 desejada = new Vector2(posicao)
            .sub(ameaca)
            .nor()
            .scl(maxVelocidade);

        return desejada.sub(velocidade);
    }

    // ARRIVE: desacelera quando entra no raio de chegada.
    public Vector2 arrive(Vector2 alvo, float raioDesaceleracao) {
        Vector2 desejada = new Vector2(alvo).sub(posicao);
        float distancia = desejada.len();

        if (distancia < 0.03f) {
            return new Vector2(velocidade).scl(-1f);
        }

        float velocidadeDesejada = maxVelocidade;
        if (distancia < raioDesaceleracao) {
            velocidadeDesejada = maxVelocidade * distancia / raioDesaceleracao;
        }

        return desejada.nor().scl(velocidadeDesejada).sub(velocidade);
    }

    // WANDER: movimento aleatório suave.
    public Vector2 wander() {
        Vector2 frente;

        if (velocidade.isZero(0.001f)) {
            frente = new Vector2(1f, 0f).scl(DIST_CIRCULO);
        } else {
            frente = new Vector2(velocidade).nor().scl(DIST_CIRCULO);
        }

        anguloWander += MathUtils.random(-VARIACAO, VARIACAO);

        Vector2 deslocamento = new Vector2(RAIO_CIRCULO, 0f)
            .setAngleRad(anguloWander);

        return frente.add(deslocamento);
    }

    // Posição futura de outro agente.
    public Vector2 previsao(Agente alvo) {
        float distancia = posicao.dst(alvo.posicao);
        float tempo = distancia / maxVelocidade;
        return new Vector2(alvo.velocidade).scl(tempo).add(alvo.posicao);
    }

    // PURSUIT: seek na posição futura do alvo.
    public Vector2 pursuit(Agente alvo) {
        return seek(previsao(alvo));
    }

    // Aplica a força de steering à velocidade e atualiza a posição.
    public void mover(Vector2 steering, float delta) {
        Vector2 forca = new Vector2(steering)
            .limit(maxForca)
            .scl(1f / massa);

        ultimaForca.set(forca);

        velocidade.mulAdd(forca, delta).limit(maxVelocidade);
        posicao.mulAdd(velocidade, delta);
    }

    // Mantém o agente dentro da tela, aparecendo do outro lado quando sair.
    public void wrap(float largura, float altura) {
        if (posicao.x < 0f) posicao.x = largura;
        if (posicao.x > largura) posicao.x = 0f;
        if (posicao.y < 0f) posicao.y = altura;
        if (posicao.y > altura) posicao.y = 0f;
    }
}
