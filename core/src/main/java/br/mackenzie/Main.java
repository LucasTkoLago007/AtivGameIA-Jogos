package br.mackenzie;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Atividade prática - Game IA / Steering Behaviors
 *
 * 1 = Seek + Arrive
 * 2 = Wander + FSM + Pursuit
 * 3 = Flocking (bônus)
 */
public class Main extends ApplicationAdapter {

    private static final float WORLD_WIDTH = 16f;
    private static final float WORLD_HEIGHT = 9f;

    private enum Modo {
        SEEK_ARRIVE,
        WANDER_FSM,
        FLOCKING
    }

    private FitViewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Modo modo = Modo.SEEK_ARRIVE;

    // Agente principal usado nos modos 1 e 2.
    private Agente agente;

    // Alvo definido pelo clique no modo Seek/Arrive.
    private final Vector2 alvoClique = new Vector2();
    private boolean possuiAlvo = false;

    // O mouse representa o jogador no modo Wander + FSM.
    private Agente jogador;
    private final Vector2 mouseAnterior = new Vector2();

    // Bando do bônus.
    private final Array<Agente> bando = new Array<>();

    private static final float RAIO_VISAO = 4f;
    private static final float RAIO_DESACELERACAO = 2f;

    @Override
    public void create() {
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();   
        font.getData().setScale(1.2f);

        agente = new Agente(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
        jogador = new Agente(3f, 3f);
        jogador.maxVelocidade = 6f;

        mouseAnterior.set(jogador.posicao);

        criarBando();
    }

    private void criarBando() {
        bando.clear();

        for (int i = 0; i < 30; i++) {
            Agente a = new Agente(
                MathUtils.random(1f, WORLD_WIDTH - 1f),
                MathUtils.random(1f, WORLD_HEIGHT - 1f)
            );

            a.maxVelocidade = MathUtils.random(2.0f, 3.0f);
            a.maxForca = 5f;
            a.raio = 0.12f;
            bando.add(a);
        }
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        lerTeclas();

        switch (modo) {
            case SEEK_ARRIVE:
                atualizarSeekArrive(delta);
                break;
            case WANDER_FSM:
                atualizarWanderFSM(delta);
                break;
            case FLOCKING:
                atualizarFlocking(delta);
                break;
        }

        ScreenUtils.clear(0.07f, 0.08f, 0.11f, 1f);
        viewport.apply();

        desenharMundo();
        desenharHUD();
    }

    private void lerTeclas() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            modo = Modo.SEEK_ARRIVE;
            agente.posicao.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
            agente.velocidade.setZero();
            possuiAlvo = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            modo = Modo.WANDER_FSM;
            agente.posicao.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
            agente.velocidade.set(1f, 0f);
            agente.estado = Agente.Estado.EXPLORANDO;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            modo = Modo.FLOCKING;
            criarBando();
        }
    }

    private Vector2 mouseNoMundo() {
        Vector2 mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);
        return mouse;
    }

    // ---------------------------------------------------------------
    // 1) SEEK + ARRIVE
    // ---------------------------------------------------------------
    private void atualizarSeekArrive(float delta) {
        if (Gdx.input.justTouched()) {
            alvoClique.set(mouseNoMundo());
            possuiAlvo = true;
        }

        if (!possuiAlvo) {
            agente.ultimaForca.setZero();
            return;
        }

        float distancia = agente.posicao.dst(alvoClique);
        Vector2 steering;

        // Longe do alvo: Seek. Perto do alvo: Arrive.
        if (distancia > RAIO_DESACELERACAO) {
            steering = agente.seek(alvoClique);
        } else {
            steering = agente.arrive(alvoClique, RAIO_DESACELERACAO);
        }

        agente.mover(steering, delta);

        if (distancia < 0.05f && agente.velocidade.len() < 0.08f) {
            agente.posicao.set(alvoClique);
            agente.velocidade.setZero();
            agente.ultimaForca.setZero();
        }
    }

    // ---------------------------------------------------------------
    // 2) WANDER + FSM + PURSUIT
    // ---------------------------------------------------------------
    private void atualizarWanderFSM(float delta) {
        Vector2 mouse = mouseNoMundo();

        // Calcula uma velocidade simples do jogador a partir do movimento do mouse.
        if (delta > 0f) {
            jogador.velocidade.set(mouse)
                .sub(mouseAnterior)
                .scl(1f / delta)
                .limit(jogador.maxVelocidade);
        }

        jogador.posicao.set(mouse);
        mouseAnterior.set(mouse);

        float distanciaJogador = agente.posicao.dst(jogador.posicao);

        // Máquina de estados simples.
        if (agente.estado == Agente.Estado.EXPLORANDO && distanciaJogador < RAIO_VISAO) {
            agente.estado = Agente.Estado.PERSEGUINDO;
        } else if (agente.estado == Agente.Estado.PERSEGUINDO && distanciaJogador > RAIO_VISAO + 1f) {
            agente.estado = Agente.Estado.EXPLORANDO;
        }

        Vector2 steering;

        if (agente.estado == Agente.Estado.EXPLORANDO) {
            steering = agente.wander();
        } else {
            steering = agente.pursuit(jogador);
        }

        agente.mover(steering, delta);
        agente.wrap(WORLD_WIDTH, WORLD_HEIGHT);
    }

    // ---------------------------------------------------------------
    // 3) FLOCKING - BÔNUS
    // ---------------------------------------------------------------
    private void atualizarFlocking(float delta) {
        Array<Vector2> forcas = new Array<>();

        // Calculamos primeiro todas as forças para não alterar o resultado
        // dos próximos agentes durante o mesmo frame.
        for (int i = 0; i < bando.size; i++) {
            Agente a = bando.get(i);

            Vector2 separacao = separacao(a).scl(1.8f);
            Vector2 alinhamento = alinhamento(a).scl(1.0f);
            Vector2 coesao = coesao(a).scl(0.8f);

            Vector2 steering = new Vector2()
                .add(separacao)
                .add(alinhamento)
                .add(coesao)
                .limit(a.maxForca);

            forcas.add(steering);
        }

        for (int i = 0; i < bando.size; i++) {
            Agente a = bando.get(i);
            a.mover(forcas.get(i), delta);
            a.wrap(WORLD_WIDTH, WORLD_HEIGHT);
        }
    }

    private Vector2 separacao(Agente atual) {
        Vector2 forca = new Vector2();
        int quantidade = 0;

        for (Agente outro : bando) {
            if (outro == atual) continue;

            float d = atual.posicao.dst(outro.posicao);
            if (d > 0f && d < 0.9f) {
                Vector2 afastar = new Vector2(atual.posicao)
                    .sub(outro.posicao)
                    .nor()
                    .scl(1f / Math.max(d, 0.01f));
                forca.add(afastar);
                quantidade++;
            }
        }

        if (quantidade > 0) {
            forca.scl(1f / quantidade);
        }

        return forca;
    }

    private Vector2 alinhamento(Agente atual) {
        Vector2 media = new Vector2();
        int quantidade = 0;

        for (Agente outro : bando) {
            if (outro == atual) continue;

            float d = atual.posicao.dst(outro.posicao);
            if (d < 1.8f) {
                media.add(outro.velocidade);
                quantidade++;
            }
        }

        if (quantidade == 0) return media;

        media.scl(1f / quantidade);
        if (!media.isZero()) {
            media.nor().scl(atual.maxVelocidade);
        }

        return media.sub(atual.velocidade);
    }

    private Vector2 coesao(Agente atual) {
        Vector2 centro = new Vector2();
        int quantidade = 0;

        for (Agente outro : bando) {
            if (outro == atual) continue;

            float d = atual.posicao.dst(outro.posicao);
            if (d < 1.8f) {
                centro.add(outro.posicao);
                quantidade++;
            }
        }

        if (quantidade == 0) return centro;

        centro.scl(1f / quantidade);
        return atual.seek(centro);
    }

    // ---------------------------------------------------------------
    // DESENHO
    // ---------------------------------------------------------------
    private void desenharMundo() {
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);

        if (modo == Modo.FLOCKING) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.CYAN);
            for (Agente a : bando) {
                shapeRenderer.circle(a.posicao.x, a.posicao.y, a.raio, 16);
            }
            shapeRenderer.end();
            return;
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Agente principal.
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.circle(agente.posicao.x, agente.posicao.y, 0.28f, 24);

        if (modo == Modo.SEEK_ARRIVE && possuiAlvo) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.circle(alvoClique.x, alvoClique.y, 0.18f, 24);
        }

        if (modo == Modo.WANDER_FSM) {
            // Jogador = mouse.
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.circle(jogador.posicao.x, jogador.posicao.y, 0.25f, 24);
        }

        shapeRenderer.end();

        // Vetores de depuração, como sugerido nos slides.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Velocidade em verde.
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.line(
            agente.posicao.x,
            agente.posicao.y,
            agente.posicao.x + agente.velocidade.x * 0.45f,
            agente.posicao.y + agente.velocidade.y * 0.45f
        );

        // Força de steering em magenta.
        shapeRenderer.setColor(Color.MAGENTA);
        shapeRenderer.line(
            agente.posicao.x,
            agente.posicao.y,
            agente.posicao.x + agente.ultimaForca.x * 0.18f,
            agente.posicao.y + agente.ultimaForca.y * 0.18f
        );

        if (modo == Modo.WANDER_FSM) {
            shapeRenderer.setColor(Color.GRAY);
            shapeRenderer.circle(agente.posicao.x, agente.posicao.y, RAIO_VISAO, 60);
        }

        shapeRenderer.end();
    }

    private void desenharHUD() {
        batch.getProjectionMatrix().setToOrtho2D(
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        batch.begin();

        font.draw(batch, "1 - Seek + Arrive", 15, Gdx.graphics.getHeight() - 15);
        font.draw(batch, "2 - Wander + FSM + Pursuit", 15, Gdx.graphics.getHeight() - 35);
        font.draw(batch, "3 - Flocking (bonus)", 15, Gdx.graphics.getHeight() - 55);

        if (modo == Modo.SEEK_ARRIVE) {
            font.draw(batch, "Modo atual: SEEK / ARRIVE", 15, 45);
            font.draw(batch, "Clique em um ponto para definir o alvo.", 15, 25);
        } else if (modo == Modo.WANDER_FSM) {
            font.draw(batch, "Modo atual: WANDER + FSM", 15, 45);
            font.draw(batch, "Estado: " + agente.estado, 15, 25);
        } else {
            font.draw(batch, "Modo atual: FLOCKING - 30 agentes", 15, 25);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
