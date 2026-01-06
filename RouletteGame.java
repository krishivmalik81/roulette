import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;

public class RouletteGame extends JPanel implements ActionListener, MouseListener {

    // ===================== WINDOW =====================
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 750;

    // ===================== WHEEL ======================
    private static final int WHEEL_RADIUS = 220;
    private static final int CENTER_X = 330;
    private static final int CENTER_Y = 350;

    // European + 00 hybrid
    private static final String[] NUMBERS = {
            "0","28","9","26","30","11","7","20","32","17",
            "5","22","34","15","3","24","36","13","1",
            "00","27","10","25","29","12","8","19","31",
            "18","6","21","33","16","4","23","35","14","2"
    };

    // ===================== STATE ======================
    private double wheelAngle = 0;
    private double wheelSpeed = 0;

    private double ballAngle = 0;
    private double ballSpeed = 0;

    private boolean spinning = false;

    private int balance = 100;

    private Set<String> numberBets = new HashSet<>();

    private enum SpecialBet { NONE, RED, BLACK, ODD, EVEN }
    private SpecialBet specialBet = SpecialBet.NONE;

    private Timer timer;
    private JButton spinButton;

    // ===================== CONSTRUCTOR =====================
    public RouletteGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 110, 15));
        setLayout(null);

        spinButton = new JButton("SPIN");
        spinButton.setBounds(470, 660, 160, 45);
        spinButton.setFont(new Font("Arial", Font.BOLD, 18));
        spinButton.addActionListener(e -> spin());
        add(spinButton);

        addMouseListener(this);

        timer = new Timer(16, this);
        timer.start();
    }

    // ===================== SPIN =====================
    private void spin() {
        if (spinning) return;
        if (numberBets.isEmpty() && specialBet == SpecialBet.NONE) return;

        spinning = true;
        ballAngle = 0; // RESET BALL TO 0 DEGREES (UNIT CIRCLE)
        wheelSpeed = 0.28 + Math.random() * 0.15;
        ballSpeed = -0.55 - Math.random() * 0.25;
    }

    // ===================== UPDATE =====================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (spinning) {
            wheelAngle += wheelSpeed;
            ballAngle += ballSpeed;

            wheelSpeed *= 0.992;
            ballSpeed *= 0.985;
            
            if (Math.abs(wheelSpeed) < 0.002) {
                spinning = false;
                resolveSpin();
            }
        }
        repaint();
    }

    // ===================== RESULT =====================
    private void resolveSpin() {
        int index = (int) ((normalize(ballAngle - wheelAngle) / (2 * Math.PI)) * NUMBERS.length);
        index = (index + NUMBERS.length) % NUMBERS.length;
        String result = NUMBERS[index];

        boolean win = false;
        int payout = 0;

        if (!numberBets.isEmpty()) {
            if (numberBets.contains(result)) {
                payout = (int) Math.round((36.0 / numberBets.size()) * 2);
                win = true;
            }
        }

        if (specialBet != SpecialBet.NONE) {
            int n = (result.equals("0") || result.equals("00")) ? -1 : Integer.parseInt(result);

            switch (specialBet) {
                case RED -> win = isRed(result);
                case BLACK -> win = isBlack(result);
                case ODD -> win = (n > 0 && n % 2 == 1);
                case EVEN -> win = (n > 0 && n % 2 == 0);
            }

            if (win) payout = 1 + (int) Math.round(1 * 0.05);
        }

        if (win) {
            balance += payout;
            JOptionPane.showMessageDialog(this,
                    "WIN!\nNumber: " + result + "\n+$" + payout);
        } else {
            JOptionPane.showMessageDialog(this,
                    "LOSE!\nNumber: " + result);
        }

        numberBets.clear();
        specialBet = SpecialBet.NONE;
    }

    private double normalize(double a) {
        a %= (2 * Math.PI);
        if (a < 0) a += 2 * Math.PI;
        return a;
    }

    // ===================== DRAW =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawWheel(g2);
        drawBall(g2);
        drawGrid(g2);
        drawUI(g2);
    }

    private void drawWheel(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillOval(CENTER_X - WHEEL_RADIUS - 12, CENTER_Y - WHEEL_RADIUS - 12,
                (WHEEL_RADIUS + 12) * 2, (WHEEL_RADIUS + 12) * 2);

        AffineTransform old = g2.getTransform();
        g2.translate(CENTER_X, CENTER_Y);
        g2.rotate(wheelAngle);

        double slice = 2 * Math.PI / NUMBERS.length;

        for (int i = 0; i < NUMBERS.length; i++) {
            g2.setColor(getColor(NUMBERS[i]));
            g2.fillArc(-WHEEL_RADIUS, -WHEEL_RADIUS,
                    WHEEL_RADIUS * 2, WHEEL_RADIUS * 2,
                    (int) Math.toDegrees(i * slice),
                    (int) Math.toDegrees(slice));

            g2.setColor(Color.WHITE);
            g2.rotate(slice / 2);
            g2.drawString(NUMBERS[i], WHEEL_RADIUS - 35, 5);
            g2.rotate(-slice / 2);
            g2.rotate(slice);
        }

        g2.setTransform(old);
    }

    private void drawBall(Graphics2D g2) {
        int r = WHEEL_RADIUS - 25;
        int x = CENTER_X + (int) (Math.cos(ballAngle) * r);
        int y = CENTER_Y + (int) (Math.sin(ballAngle) * r);

        g2.setColor(Color.WHITE);
        g2.fillOval(x - 7, y - 7, 14, 14);
    }

    private void drawGrid(Graphics2D g2) {
        int startX = 620;
        int startY = 80;
        int cellW = 60;
        int cellH = 40;

        for (int i = 0; i < NUMBERS.length; i++) {
            int row = i / 6;
            int col = i % 6;

            int x = startX + col * cellW;
            int y = startY + row * cellH;

            g2.setColor(getColor(NUMBERS[i]));
            g2.fillRect(x, y, cellW, cellH);

            if (numberBets.contains(NUMBERS[i])) {
                g2.setColor(Color.YELLOW);
                g2.drawRect(x + 3, y + 3, cellW - 6, cellH - 6);
            }

            g2.setColor(Color.WHITE);
            g2.drawString(NUMBERS[i], x + 22, y + 25);
        }

        drawSpecialBet(g2, "RED", 620, 520, SpecialBet.RED, Color.RED);
        drawSpecialBet(g2, "BLACK", 740, 520, SpecialBet.BLACK, Color.BLACK);
        drawSpecialBet(g2, "ODD", 620, 580, SpecialBet.ODD, Color.DARK_GRAY);
        drawSpecialBet(g2, "EVEN", 740, 580, SpecialBet.EVEN, Color.DARK_GRAY);
    }

    private void drawSpecialBet(Graphics2D g2, String text, int x, int y,
                                SpecialBet bet, Color color) {
        g2.setColor(color);
        g2.fillRect(x, y, 100, 45);

        if (specialBet == bet) {
            g2.setColor(Color.YELLOW);
            g2.drawRect(x + 3, y + 3, 94, 39);
        }

        g2.setColor(Color.WHITE);
        g2.drawString(text, x + 30, y + 28);
    }

    private void drawUI(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("Balance: $" + balance, 40, 40);
        g2.drawString("Current Bet: $" + getTotalBet(), 40, 75);
    }

    private int getTotalBet() {
        return (specialBet != SpecialBet.NONE) ? 1 : numberBets.size();
    }

    // ===================== INPUT =====================
    @Override
    public void mouseClicked(MouseEvent e) {
        if (spinning || balance <= 0) return;

        Point p = e.getPoint();

        int startX = 620;
        int startY = 80;
        int cellW = 60;
        int cellH = 40;

        if (specialBet == SpecialBet.NONE) {
            for (int i = 0; i < NUMBERS.length; i++) {
                int row = i / 6;
                int col = i % 6;

                Rectangle r = new Rectangle(
                        startX + col * cellW,
                        startY + row * cellH,
                        cellW, cellH
                );

                if (r.contains(p) && !numberBets.contains(NUMBERS[i])) {
                    numberBets.add(NUMBERS[i]);
                    balance--;
                }
            }
        }

        if (clickSpecial(p, 620, 520)) setSpecial(SpecialBet.RED);
        if (clickSpecial(p, 740, 520)) setSpecial(SpecialBet.BLACK);
        if (clickSpecial(p, 620, 580)) setSpecial(SpecialBet.ODD);
        if (clickSpecial(p, 740, 580)) setSpecial(SpecialBet.EVEN);
    }

    private boolean clickSpecial(Point p, int x, int y) {
        return new Rectangle(x, y, 100, 45).contains(p);
    }

    private void setSpecial(SpecialBet bet) {
        if (specialBet == bet || balance <= 0) return;
        numberBets.clear();
        specialBet = bet;
        balance--;
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // ===================== COLORS =====================
    private Color getColor(String num) {
        if (num.equals("0") || num.equals("00")) return Color.GREEN;
        return isRed(num) ? Color.RED : Color.BLACK;
    }

    private boolean isRed(String num) {
        if (num.equals("0") || num.equals("00")) return false;
        int n = Integer.parseInt(num);
        return n % 2 == 1;
    }

    private boolean isBlack(String num) {
        if (num.equals("0") || num.equals("00")) return false;
        int n = Integer.parseInt(num);
        return n % 2 == 0;
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Roulette");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new RouletteGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
