package com.chess.gui.art;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates all 12 Staunton-inspired chess piece PNG images (100×100px).
 */
public class PieceArtGenerator {

    static final int SZ  = 100;
    static final int CX  = SZ / 2;

    // White palette
    static final Color W_BASE   = new Color(255, 249, 235);
    static final Color W_MID    = new Color(230, 210, 170);
    static final Color W_DARK   = new Color(180, 150, 100);
    static final Color W_INK    = new Color(90,  65,  30);

    // Black palette
    static final Color B_BASE   = new Color(55,  38,  18);
    static final Color B_MID    = new Color(35,  22,  8);
    static final Color B_LIGHT  = new Color(110, 80,  45);
    static final Color B_INK    = new Color(180, 145, 90);

    public static void main(String[] args) throws Exception {
        String dir = "src/com/chess/gui/art";
        new File(dir).mkdirs();
        String[] colours = {"white","black"};
        String[] types   = {"king","queen","rook","bishop","knight","pawn"};
        for (String col : colours) {
            boolean w = col.equals("white");
            for (String type : types) {
                BufferedImage img = draw(type, w);
                File f = new File(dir + "/" + col + "_" + type + ".png");
                ImageIO.write(img, "PNG", f);
                System.out.println("Written: " + f);
            }
        }
    }

    static BufferedImage draw(String type, boolean white) {
        BufferedImage img = new BufferedImage(SZ, SZ, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hint(g);
        switch (type) {
            case "pawn"   -> pawn(g, white);
            case "rook"   -> rook(g, white);
            case "knight" -> knight(g, white);
            case "bishop" -> bishop(g, white);
            case "queen"  -> queen(g, white);
            case "king"   -> king(g, white);
        }
        g.dispose();
        return img;
    }

    static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    // ── Shared drawing utilities ───────────────────────────────────────────────

    /** Soft drop-shadow before filling a shape. */
    static void shadow(Graphics2D g, Shape s, int dx, int dy, float blur) {
        g.setColor(new Color(0, 0, 0, 55));
        AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
        g.setStroke(new BasicStroke(blur * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.fill(at.createTransformedShape(s));
        g.draw(at.createTransformedShape(s));
    }

    /** Fill with radial gradient + top sheen, then stroke outline. */
    static void paint(Graphics2D g, Shape s, boolean white) {
        Rectangle2D b = s.getBounds2D();
        float cx = (float)(b.getCenterX()), cy = (float)(b.getCenterY());
        float r  = (float)(Math.max(b.getWidth(), b.getHeight()) * 0.55);

        // Radial fill
        RadialGradientPaint fill = new RadialGradientPaint(
            cx - (float)b.getWidth()*0.15f, cy - (float)b.getHeight()*0.2f, r,
            new float[]{0f, 0.55f, 1f},
            white ? new Color[]{W_BASE, W_MID, W_DARK}
                  : new Color[]{B_LIGHT, B_BASE, B_MID}
        );
        g.setPaint(fill);
        g.fill(s);

        // Top-left specular sheen
        GradientPaint sheen = new GradientPaint(
            (float)b.getMinX(), (float)b.getMinY(),
            white ? new Color(255,255,255,140) : new Color(255,220,150,60),
            (float)b.getCenterX(), (float)b.getCenterY(),
            new Color(255,255,255,0)
        );
        g.setPaint(sheen);
        g.fill(s);

        // Outline
        g.setPaint(null);
        g.setColor(white ? W_INK : B_INK);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    /** Wide elliptical base at bottom. */
    static void base(Graphics2D g, boolean white, double y, double w, double h) {
        Shape s = new RoundRectangle2D.Double(CX - w/2, y, w, h, h*0.6, h*0.6);
        shadow(g, s, 2, 3, 2.5f);
        paint(g, s, white);
    }

    /** Horizontal shelf (stem connector). */
    static void shelf(Graphics2D g, boolean white, double y, double w, double h) {
        Shape s = new RoundRectangle2D.Double(CX - w/2, y, w, h, h*0.5, h*0.5);
        paint(g, s, white);
    }

    /** Circle head. */
    static void head(Graphics2D g, boolean white, double cx, double cy, double r) {
        Shape s = new Ellipse2D.Double(cx - r, cy - r, r*2, r*2);
        shadow(g, s, 2, 2, 2f);
        paint(g, s, white);
    }

    // ── Pawn ──────────────────────────────────────────────────────────────────

    static void pawn(Graphics2D g, boolean white) {
        base(g, white, 76, 58, 14);
        // stem
        GeneralPath stem = new GeneralPath();
        stem.moveTo(CX - 8, 76);
        stem.curveTo(CX - 10, 62, CX - 7, 56, CX - 5, 50);
        stem.lineTo(CX + 5, 50);
        stem.curveTo(CX + 7, 56, CX + 10, 62, CX + 8, 76);
        stem.closePath();
        paint(g, stem, white);
        // head
        head(g, white, CX, 38, 16);
    }

    // ── Rook ──────────────────────────────────────────────────────────────────

    static void rook(Graphics2D g, boolean white) {
        base(g, white, 78, 62, 13);
        // body
        GeneralPath body = new GeneralPath();
        body.moveTo(CX - 18, 78);
        body.curveTo(CX - 20, 60, CX - 16, 50, CX - 14, 43);
        body.lineTo(CX + 14, 43);
        body.curveTo(CX + 16, 50, CX + 20, 60, CX + 18, 78);
        body.closePath();
        shadow(g, body, 2, 2, 2f);
        paint(g, body, white);
        // parapet shelf
        shelf(g, white, 32, 40, 12);
        // 3 merlons
        double mW = 9, mH = 14, gap = 3;
        double totalW = 3*mW + 2*gap;
        double startX = CX - totalW/2;
        for (int i = 0; i < 3; i++) {
            Shape m = new RoundRectangle2D.Double(startX + i*(mW+gap), 18, mW, mH, 3, 3);
            paint(g, m, white);
        }
    }

    // ── Bishop ────────────────────────────────────────────────────────────────

    static void bishop(Graphics2D g, boolean white) {
        base(g, white, 78, 54, 13);
        // body/skirt
        GeneralPath body = new GeneralPath();
        body.moveTo(CX - 17, 78);
        body.curveTo(CX - 20, 60, CX - 12, 48, CX - 8, 42);
        body.curveTo(CX - 4, 38, CX + 4, 38, CX + 8, 42);
        body.curveTo(CX + 12, 48, CX + 20, 60, CX + 17, 78);
        body.closePath();
        shadow(g, body, 2, 2, 2f);
        paint(g, body, white);
        // collar band
        shelf(g, white, 38, 22, 7);
        // orb / head
        head(g, white, CX, 28, 10);
        // mitre finial — teardrop point
        GeneralPath tip = new GeneralPath();
        tip.moveTo(CX - 4, 18);
        tip.curveTo(CX - 3, 12, CX + 3, 12, CX + 4, 18);
        tip.curveTo(CX + 2, 22, CX - 2, 22, CX - 4, 18);
        tip.closePath();
        paint(g, tip, white);
        // notch line across orb
        g.setColor(white ? W_INK : B_INK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(CX - 8, 30, CX + 8, 30));
    }

    // ── Queen ─────────────────────────────────────────────────────────────────

    static void queen(Graphics2D g, boolean white) {
        base(g, white, 79, 66, 13);
        // full dress body
        GeneralPath body = new GeneralPath();
        body.moveTo(CX - 20, 79);
        body.curveTo(CX - 24, 60, CX - 14, 46, CX - 8, 40);
        body.curveTo(CX - 4, 36, CX + 4, 36, CX + 8, 40);
        body.curveTo(CX + 14, 46, CX + 24, 60, CX + 20, 79);
        body.closePath();
        shadow(g, body, 2, 2, 2f);
        paint(g, body, white);
        // collar
        shelf(g, white, 36, 20, 7);
        // head/orb
        head(g, white, CX, 27, 10);
        // crown band
        shelf(g, white, 14, 24, 6);
        // 5 crown balls
        double[] bx = {CX - 12, CX - 6, CX, CX + 6, CX + 12};
        double[] by = {8, 6, 5, 6, 8};
        double[] br = {3.2, 3.2, 3.8, 3.2, 3.2};
        for (int i = 0; i < bx.length; i++) {
            Shape ball = new Ellipse2D.Double(bx[i] - br[i], by[i] - br[i], br[i]*2, br[i]*2);
            shadow(g, ball, 1, 1, 1f);
            paint(g, ball, white);
        }
    }

    // ── King ──────────────────────────────────────────────────────────────────

    static void king(Graphics2D g, boolean white) {
        base(g, white, 79, 66, 13);
        // body
        GeneralPath body = new GeneralPath();
        body.moveTo(CX - 20, 79);
        body.curveTo(CX - 24, 58, CX - 14, 44, CX - 8, 38);
        body.curveTo(CX - 4, 34, CX + 4, 34, CX + 8, 38);
        body.curveTo(CX + 14, 44, CX + 24, 58, CX + 20, 79);
        body.closePath();
        shadow(g, body, 2, 2, 2f);
        paint(g, body, white);
        // collar
        shelf(g, white, 34, 20, 7);
        // head
        head(g, white, CX, 25, 10);
        // crown band
        shelf(g, white, 13, 24, 5);
        // cross — vertical bar
        Shape vbar = new RoundRectangle2D.Double(CX - 3, 3, 6, 18, 3, 3);
        paint(g, vbar, white);
        // cross — horizontal bar
        Shape hbar = new RoundRectangle2D.Double(CX - 9, 8, 18, 5, 3, 3);
        paint(g, hbar, white);
    }

    // ── Knight (proper horse-head silhouette) ─────────────────────────────────

    static void knight(Graphics2D g, boolean white) {
        base(g, white, 78, 62, 13);

        // Horse head built from carefully tuned bezier curves
        // (facing left — classic Staunton orientation)
        GeneralPath h = new GeneralPath();

        // Start bottom-right of neck where it meets the base shelf
        h.moveTo(CX + 14, 78);
        // Right side of neck, up
        h.curveTo(CX + 16, 68, CX + 14, 60, CX + 10, 55);
        // Chin / jaw
        h.curveTo(CX + 14, 52, CX + 16, 46, CX + 12, 42);
        // Muzzle front
        h.curveTo(CX + 8,  36, CX + 5,  33, CX + 6,  30);
        // Nose bridge
        h.curveTo(CX + 7,  27, CX + 5,  24, CX + 2,  22);
        // Forehead
        h.curveTo(CX - 2, 17, CX - 6, 14, CX - 4,  10);
        // Ear tip
        h.curveTo(CX - 2,  5, CX + 4,   4, CX + 6,   8);
        // Back of ear down
        h.curveTo(CX + 8, 12, CX + 4,  16, CX + 3,  20);
        // Poll / top of head right side
        h.curveTo(CX + 6, 18, CX + 10, 20, CX + 10, 26);
        // Back of neck (right side going down)
        h.curveTo(CX + 10, 32, CX + 8,  40, CX + 6,  46);
        // Lower neck back
        h.curveTo(CX + 4,  52, CX + 2,  60, CX + 2,  68);
        // Bottom left of neck base
        h.lineTo(CX - 10, 78);
        h.closePath();

        shadow(g, h, 3, 3, 3f);
        paint(g, h, white);

        // Eye
        double ex = CX + 5, ey = 27;
        Shape eye = new Ellipse2D.Double(ex - 3, ey - 3, 6, 6);
        g.setColor(white ? W_INK : B_INK);
        g.fill(eye);
        g.setColor(white ? new Color(255,240,200) : new Color(200,160,90));
        g.fill(new Ellipse2D.Double(ex - 1.2, ey - 1.5, 2.5, 2.5));

        // Nostril
        g.setColor(white ? W_INK : B_INK);
        g.fill(new Ellipse2D.Double(CX + 8, 39, 3.5, 2.5));

        // Mane line (back of neck detail)
        g.setColor(white ? W_DARK : B_LIGHT);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath mane = new GeneralPath();
        mane.moveTo(CX - 1, 22);
        mane.curveTo(CX - 4, 30, CX - 3, 42, CX - 2, 52);
        mane.curveTo(CX - 1, 60, CX + 0, 68, CX + 1, 74);
        g.draw(mane);
    }
}
