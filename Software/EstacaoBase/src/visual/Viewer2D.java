package visual;

import java.awt.Event;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import processing.core.PApplet;
import processing.core.PVector;

/**
 * Classe responsável por exibir os objetos Drawable2D. Possui recursos de pan,
 * zoom e rotate. pan: arrastar o mouse com botão esquerdo pressionado; zoom:
 * rodar a roda do mouse OU pressionar setas do teclado para cima/baixo. rotate:
 * rodar a roda do mouse com CTRL pressionado OU pressionar setas do teclado
 * para esquerda/direita.
 *
 * @author stefan
 * @see Drawable2D
 */
public class Viewer2D extends PApplet {

    private ArrayList<Drawable2D> listaDrawable2D = new ArrayList<Drawable2D>();
    private ArrayList<MouseListener2D> listaMouseListener2D = new ArrayList<MouseListener2D>();
    //Escala em px/mm
    //escala=d/D
    private float escala = 0.1f;
    private float escala_step = 0.02f;
    //Angulo do viewport (radianos). Zero grau significa eixo X para a DIREITA e Y para BAIXO. Angulos aumentam no sentido horário.
    private float angulo_visao = 0;
    private float angulo_step;
    private PVector origemRealNaInterface = new PVector(0, 0);
    private boolean indicadorCentro = true;
    private boolean mouseLeftPressed = false;
    private boolean controlPressed = false;
    private PVector pressPos = new PVector(0, 0);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[]{"visual.Viewer2D"});

    }

    public Viewer2D() {
    }

    public Viewer2D(Drawable2D drawable) {
        listaDrawable2D.add(drawable);
    }

    @Override
    public void setup() {

        size(800, 600);
        if (frame != null) {
            frame.setResizable(true);
        }
        angulo_step = radians(5);
        //Desabilita o loop automatico do metodo draw(). O metodo redraw() deve ser chamado caso seja necessário.
        noLoop();
        //Muda a FPS
        frameRate(60);

        //Adiciona um listener para verificar rotações na roda do mouse. Usa o método mouseWheel() para tratar o evento.
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mouseWheel(evt.getWheelRotation());
            }
        });
    }

    /**
     * Método executado a cada vez que a interface precise ser atualizada. Não
     * deve ser chamado diretamente, mas somente pelo método redraw().
     */
    @Override
    public void draw() {
        background(255);
        pushMatrix();
        pushStyle();
        //
        // Mostra as coordenadas reais do mouse no canto inferior esquerdo
        //
        PVector posMouseInterface = new PVector(mouseX, mouseY);
        posMouseInterface.sub(new PVector(width / 2, height / 2));
        PVector posMouseReal = new PVector(mouseX, mouseY);
        posMouseReal.sub(new PVector(width / 2, height / 2));
        posMouseReal.sub(origemRealNaInterface);
        posMouseReal.rotate(-angulo_visao);
        posMouseReal.div(escala);
        fill(0);
        textAlign(LEFT);
//        text("oi",height-20, 20);
        text(String.format("mouse_interface: (%.2f, %.2f) px", posMouseInterface.x, posMouseInterface.y), 5, height - 25);
        text(String.format("mouse_real: (%.2f, %.2f) cm", posMouseReal.x / 10, posMouseReal.y / 10), 5, height - 10);
        popMatrix();
        popStyle();

        //
        // Move a visualização se o mouse for arrastado
        //
        if (mouseLeftPressed) {
            origemRealNaInterface.x += mouseX - pressPos.x;
            origemRealNaInterface.y += mouseY - pressPos.y;
            pressPos.x = mouseX;
            pressPos.y = mouseY;
        }

        pushMatrix();
        pushStyle();

        //
        // Mostra as informações no canto superior esquerdo
        //
        fill(0);
//        text(String.format("origemRealNaInterface.x=%.2f px", origemRealNaInterface.x), 5, 15);
//        text(String.format("origemRealNaInterface.y=%.2f px", origemRealNaInterface.y), 5, 30);
        text(String.format("origemRealNaInterface:(%.2f, %.2f) px", origemRealNaInterface.x, origemRealNaInterface.y), 5, 15);
        text(String.format("angulo_visao=%.2f deg", degrees(angulo_visao)), 5, 30);
        text(String.format("escala=%.2f px/mm", escala), 5, 45);
        text(String.format("frameRate=%.2f fps", frameRate), 5, 60);
        //text(String.format("robo.", ), 5, 80);


        //
        //Desenha o indicador de centro da tela
        //
        if (indicadorCentro) {
            translate((float) width / 2, (float) height / 2);
            fill(0, 100);
            stroke(0, 100);
            ellipse(0, 0, 5, 5);
            //line(-10, 0, 15, 0);
            //line(0, -10, 0, 10);
            arrowLine(-0, 0, 15, 0, 0, radians(20), false);
            arrowLine(0, 0, 0, 15, 0, radians(20), false);
        }

        popStyle();
        popMatrix();


        //
        //Desenha todos os Drawable2D
        //
        for (int i = 0; i < listaDrawable2D.size(); i++) {
            listaDrawable2D.get(i).draw2D(this);
        }
        //
        //Executa todos os MouseListener2D
        //
        for (int i = 0; i < listaMouseListener2D.size(); i++) {
            listaMouseListener2D.get(i).mouseChanged(this, posMouseReal);
        }

//        stateChanged = false;
    }

    /**
     * Efetua as transformações de coordenadas (translação e rotação) de forma
     * que a origem (0,0) para novos desenhos seja alinhada com a origem (0,0)
     * real e os desenhos sejam rotacionados de acordo com angulo_visao.
     */
    public void transform() {
        transform_translation();
        transform_rotation();
    }

    /**
     * Efetua as transformações de coordenadas (translação) de forma que a
     * origem (0,0) para novos desenhos seja alinhada com a origem (0,0) real.
     */
    public void transform_translation() {
        translate((float) width / 2, (float) height / 2);
        translate((float) origemRealNaInterface.x, (float) origemRealNaInterface.y);
    }

    /**
     * Efetua a transformação de rotação na interface, de modo que os desenhos
     * sejam vistos rotacionados de acordo com o ângulo especificado por
     * angulo_visao.
     */
    public void transform_rotation() {
        rotate(angulo_visao);
    }

    /*
     * Draws a lines with arrows of the given angles at the ends.
     * x0 - starting x-coordinate of line
     * y0 - starting y-coordinate of line
     * x1 - ending x-coordinate of line
     * y1 - ending y-coordinate of line
     * startAngle - angle of arrow at start of line (in radians)
     * endAngle - angle of arrow at end of line (in radians)
     * solid - true for a solid arrow; false for an "open" arrow
     */
    void arrowLine(float x0, float y0, float x1, float y1,
                   float startAngle, float endAngle, boolean solid) {
        line(x0, y0, x1, y1);
        if (startAngle != 0) {
            arrowhead(x0, y0, atan2(y1 - y0, x1 - x0), startAngle, solid);
        }
        if (endAngle != 0) {
            arrowhead(x1, y1, atan2(y0 - y1, x0 - x1), endAngle, solid);
        }
    }

    /*
     * Draws an arrow head at given location
     * x0 - arrow vertex x-coordinate
     * y0 - arrow vertex y-coordinate
     * lineAngle - angle of line leading to vertex (radians)
     * arrowAngle - angle between arrow and line (radians)
     * solid - true for a solid arrow, false for an "open" arrow
     */
    void arrowhead(float x0, float y0, float lineAngle,
                   float arrowAngle, boolean solid) {
        float phi;
        float x2;
        float y2;
        float x3;
        float y3;
        final float SIZE = 8;

        x2 = x0 + SIZE * cos(lineAngle + arrowAngle);
        y2 = y0 + SIZE * sin(lineAngle + arrowAngle);
        x3 = x0 + SIZE * cos(lineAngle - arrowAngle);
        y3 = y0 + SIZE * sin(lineAngle - arrowAngle);
        if (solid) {
            triangle(x0, y0, x2, y2, x3, y3);
        } else {
            line(x0, y0, x2, y2);
            line(x0, y0, x3, y3);
        }
    }

    /**
     * Adiciona um objeto na visualização
     *
     * @param obj
     */
    public void addDrawable2D(Drawable2D obj) {
        listaDrawable2D.add(obj);
        redraw();
    }

    /**
     * Remove um objeto da visualização
     *
     * @param obj
     */
    public void removeDrawable2D(Drawable2D obj) {
        listaDrawable2D.remove(obj);
        redraw();
    }

    /**
     * Adiciona um MouseListener2D
     *
     * @param obj
     */
    public void addMouseListener2D(MouseListener2D obj) {
        listaMouseListener2D.add(obj);
        redraw();
    }

    /**
     * Remove um MouseListener2D
     *
     * @param obj
     */
    public void removeMouserListener2D(MouseListener2D obj) {
        listaMouseListener2D.remove(obj);
        redraw();
    }

    public float getEscala() {
        return escala;
    }

    public void setEscala(float escala) {
        this.escala = escala;
        redraw();
    }

    public float getAngulo() {
        return angulo_visao;
    }

    public void setAngulo(float angulo) {
        this.angulo_visao = angulo;
        redraw();
    }

    /**
     * Efetua zoom na interface, fazendo escala = escala + step. Move a origem
     * real de modo que o zoom seja feito no centro de visualização da
     * interface.
     *
     * @param step Quantidade a ser adicionada à escala
     */
    public void zoom_view(float step) {
        if (step > 0 || escala > 0.1f) {
            origemRealNaInterface.setMag(origemRealNaInterface.mag() * ((float) 1 + step / escala));
            setEscala(escala + step);
        }
    }

    /**
     * Rotaciona a visão, fanzendo angulo_visao = angulo_visao + step. Move a
     * origem real de modo que a rotação seja feito no centro de visualização da
     * interface.
     *
     * @param step
     */
    public void rotate_view(float step) {
        setAngulo(angulo_visao + step);
        origemRealNaInterface.rotate(step);
    }

    @Override
    public void keyPressed() {
        super.keyPressed();
        System.out.println("keyCode: " + keyCode);
        switch (keyCode) {
            case RIGHT:
                rotate_view(angulo_step);
                break;
            case LEFT:
                rotate_view(-angulo_step);
                break;
            case UP:
                zoom_view(escala_step);
                break;
            case DOWN:
                zoom_view(-escala_step);
                break;
            case 32: //SPACE
                //1ª vez: restaura a origem real para (0,0) na visualização.
                //2ª vez: restaura o angulo de visão para 0.
                //3ª vez: restaura a escala para 1.
                if (origemRealNaInterface.x == 0 && origemRealNaInterface.y == 0) {
                    if (angulo_visao == 0) {
                        setEscala(0.1f);
                    }
                    setAngulo(0);
                }
                origemRealNaInterface.x = (0);
                origemRealNaInterface.y = (0);
                redraw();
                break;
            case CONTROL:
                controlPressed = true;
                break;

        }
    }

    @Override
    public void keyReleased() {
        if (keyCode == CONTROL) {
            controlPressed = false;
        }
    }

    @Override
    public void mousePressed() {
        if (mouseButton == LEFT) {
            mouseLeftPressed = true;
            pressPos.x = mouseX;
            pressPos.y = mouseY;
        }
    }

    @Override
    public void mouseDragged() {
        if (mouseLeftPressed) redraw();
    }

    @Override
    public void mouseReleased() {
        if (mouseButton == LEFT) {
            mouseLeftPressed = false;
        }
    }

    @Override
    public void mouseMoved() {
        redraw();
    }

    /**
     * Efetua zoom e rotação na interface de acordo com o movimento da roda do
     * mouse.
     *
     * @param delta Maior que 0: roda para cima. Menor que 0: roda para baixo.
     */
    public void mouseWheel(int delta) {
        if (delta < 0) {
            if (controlPressed) rotate_view(angulo_step);
            else zoom_view(escala_step);
        } else {
            if (controlPressed) rotate_view(-angulo_step);
            else zoom_view(-escala_step);

        }
    }
}
