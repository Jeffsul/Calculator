package com.jeffsul.calculator;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class Calculator extends JFrame
{
  private static final double VERSION = 2.0;
  
  private static final int WIDTH = 900;
  private static final int HEIGHT = 720;
  
  private static final int GRAPH_WIDTH = 600;
  private static final int GRAPH_HEIGHT = 500;
  private static final int GRAPH_FULL_WIDTH = 1000;
  private static final int GRAPH_FULL_HEIGHT = 1000;
  
  private static final String OPERATORS = "+-/*^()";
  private static final Font LOADING_FONT = new Font("Arial", Font.BOLD, 32);
  private static final Font AXIS_FONT = new Font("Arial", Font.PLAIN, 9);
  
  private static final int MARGIN = 10;
  
  private static final BasicStroke STROKE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  
  private static NumberFormat numberFormat = NumberFormat.getNumberInstance();
  
  private double lastAnswer = 0.0;
  private ArrayList<String> history;
  private int historyIndex = 0;
  
  private JPanel graphPanel = new JPanel();
  private JComboBox eqnComboBox;
  private JTextArea outputTextArea;
  
  private Graphics2D dbg;
  private Image dbImage;
  
  private GeneralPath graphPath = new GeneralPath();
  
  private Graphics2D g;
  
  private ArrayList<String> functions = new ArrayList<String>();
  private Color[] colours = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.DARK_GRAY, Color.CYAN};
  
  private boolean radianMode = true;
  
  private Point offset = new Point(0, 0);
  private Point coord = new Point(0, 0);
  private Point2D.Double zoom = new Point2D.Double(15, 15);
  private Point lastClick = new Point(-100, -100);
  
  public Calculator()
  {
    super("JCalculator " + VERSION);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setPreferredSize(new Dimension(WIDTH, HEIGHT));
    setResizable(false);
    setLayout(new BorderLayout());
    
    final JTabbedPane tabPane = new JTabbedPane();

    outputTextArea = new JTextArea(30, 30);
    outputTextArea.setMargin(new Insets(MARGIN, MARGIN, MARGIN, MARGIN));
    outputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
    outputTextArea.setAutoscrolls(true);
    outputTextArea.setEditable(false);
    JScrollPane sp = new JScrollPane(outputTextArea);
    tabPane.addTab("Calculator", sp);
    
    final JTextField inputTextField = new JTextField(70);
    inputTextField.setFont(new Font("Monospaced", Font.PLAIN, 14));
    ActionListener enterActionListener = new ActionListener() 
    {
      public void actionPerformed(ActionEvent event)
      {
        if (tabPane.getSelectedIndex() == 0)
          calculate(inputTextField.getText());
        else
        {
          String eqn = inputTextField.getText();
          functions.add(eqn);
          eqnComboBox.addItem(eqn);
          if (!graph(eqn, colours[(functions.size()-1)%colours.length]))
          {
            functions.remove(eqn);
            eqnComboBox.removeItem(eqn);
          }
          inputTextField.requestFocus();
        }
        inputTextField.setText("");
      }
    };
    inputTextField.addActionListener(enterActionListener);
    inputTextField.addKeyListener(new KeyAdapter()
    {
      public void keyPressed(KeyEvent event)
      {
        switch (event.getKeyCode())
        {
          case KeyEvent.VK_UP:
            if (historyIndex > 0)
              historyIndex--;
            break;
          case KeyEvent.VK_DOWN:
            if (historyIndex < history.size() - 1)
              historyIndex++;
            break;
          default:
            return;
        }
        inputTextField.setText(history.get(historyIndex));
      }
    });
    JButton equalButton = new JButton("Enter");
    equalButton.addActionListener(enterActionListener);
    
    final JComboBox angleModeCombo = new JComboBox(new String[] {"Radians", "Degrees"});
    angleModeCombo.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent event) 
      {
        radianMode = angleModeCombo.getSelectedIndex() == 0;
      }
    });
    
    JPanel inputPnl = new JPanel();
    inputPnl.add(new JLabel("Input:"));
    inputPnl.add(inputTextField);
    inputPnl.add(equalButton);
    inputPnl.add(new JLabel(" Angle Mode:"));
    inputPnl.add(angleModeCombo);
    add(inputPnl, BorderLayout.PAGE_END);
    
    history = new ArrayList<String>();
    numberFormat.setMaximumFractionDigits(8);
    inputTextField.requestFocus();
    
    Font font = new Font("Arial", Font.PLAIN, 10);
    
    JPanel graphingPnl = new JPanel();
    graphPanel.setLayout(new BorderLayout());
    JPanel graphOptionPnl = new JPanel();
    JLabel xZoomLbl = new JLabel("X Zoom:");
    xZoomLbl.setFont(font);
    graphOptionPnl.add(xZoomLbl);
    final JSlider xZoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 75, 10);
    xZoomSlider.setMajorTickSpacing(10);
    xZoomSlider.setMinorTickSpacing(5);
    xZoomSlider.setPreferredSize(new Dimension(120, 50));
    xZoomSlider.setSnapToTicks(true);
    xZoomSlider.setPaintTicks(true);
    xZoomSlider.setPaintLabels(true);
    xZoomSlider.setFont(font);
    xZoomSlider.addMouseListener(new MouseAdapter()
    {
      public void mouseReleased(MouseEvent e)
      {
        zoom.x = xZoomSlider.getValue() + 5;
        initGraph();
      }
    });
    graphOptionPnl.add(xZoomSlider);
    
    JLabel yZoomLbl = new JLabel("Y Zoom:");
    yZoomLbl.setFont(font);
    graphOptionPnl.add(yZoomLbl);
    final JSlider yZoomSlider = new JSlider(JSlider.HORIZONTAL, 0, 75, 10);
    yZoomSlider.setMajorTickSpacing(10);
    yZoomSlider.setMinorTickSpacing(5);
    yZoomSlider.setPreferredSize(new Dimension(120, 50));
    yZoomSlider.setSnapToTicks(true);
    yZoomSlider.setPaintTicks(true);
    yZoomSlider.setPaintLabels(true);
    yZoomSlider.setFont(font);
    yZoomSlider.addMouseListener(new MouseAdapter()
    {
      public void mouseReleased(MouseEvent e)
      {
        zoom.y = yZoomSlider.getValue() + 5;
        initGraph();
      }
    });
    graphOptionPnl.add(yZoomSlider);
    
    JPanel graphOpPnl1 = new JPanel();
    JLabel eqnLbl = new JLabel("Equations:");
    eqnLbl.setFont(font);
    graphOpPnl1.add(eqnLbl);
    eqnComboBox = new JComboBox(functions.toArray());
    eqnComboBox.setFont(font);
    eqnComboBox.setPreferredSize(new Dimension(100, 25));
    graphOpPnl1.add(eqnComboBox);
    JButton editEqnBtn = new JButton("Edit");
    editEqnBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        int index = eqnComboBox.getSelectedIndex();
        String newEqn = JOptionPane.showInputDialog("Edit the equation:", functions.get(index));
        if (newEqn != null)
        {
          functions.set(index, newEqn);
          eqnComboBox.setSelectedItem(newEqn);
          initGraph();
        }
        else
          render();
        graphPanel.requestFocus();
      }
    });
    editEqnBtn.setFont(font);
    JButton deleteEqnBtn = new JButton("Delete");
    deleteEqnBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        int index = eqnComboBox.getSelectedIndex();
        functions.remove(index);
        eqnComboBox.removeItemAt(index);
        initGraph();
      }
    });
    deleteEqnBtn.setFont(font);
    JButton clearBtn = new JButton("Clear");
    clearBtn.setFont(font);
    clearBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        functions.removeAll(functions);
        eqnComboBox.removeAllItems();
        initGraph();
      }
    });
    JButton resetBtn = new JButton("Reset");
    resetBtn.setFont(font);
    resetBtn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        coord.x = 0;
        coord.y = 0;
        initGraph();
      }
    });
    graphOpPnl1.add(deleteEqnBtn);
    graphOpPnl1.add(editEqnBtn);
    graphOpPnl1.add(clearBtn);
    
    JPanel graphOpPnl2 = new JPanel();
    final JLabel xyCoordLbl = new JLabel();
    graphOpPnl2.add(xyCoordLbl);
    graphOpPnl1.add(resetBtn);
    
    JPanel graphOpPnl = new JPanel();
    graphOpPnl.setLayout(new BoxLayout(graphOpPnl, BoxLayout.Y_AXIS));
    graphOpPnl.add(graphOpPnl2);
    graphOpPnl.add(graphOpPnl1);
    graphOptionPnl.add(graphOpPnl);
    
    graphPanel.setPreferredSize(new Dimension(GRAPH_WIDTH, GRAPH_HEIGHT));
    graphPanel.addFocusListener(new FocusListener()
    {
      public void focusGained(FocusEvent e)
      {
        render();
      }
      
      public void focusLost(FocusEvent e)
      {
        render();
      }
    });
    graphPanel.addMouseMotionListener(new MouseMotionAdapter()
    {
      public void mouseDragged(MouseEvent event) 
      {
        if (lastClick.x > 0 && lastClick.y > 0)
        {
          int oldXOffset = offset.x;
          int oldYOffset = offset.y;
          offset.x += event.getX() - lastClick.x;
          offset.y += event.getY() - lastClick.y;
          render();
          offset.x = oldXOffset;
          offset.y = oldYOffset;
        }
        else
          lastClick = new Point(event.getX(), event.getY());
      }
      
      public void mouseMoved(MouseEvent e)
      {
        xyCoordLbl.setText(numberFormat.format((e.getX()-GRAPH_WIDTH/2)/zoom.x-coord.x/zoom.x) + ", " + numberFormat.format(-((e.getY()-GRAPH_HEIGHT/2)/zoom.y-coord.y/zoom.y)));
      }
    });
    graphPanel.addMouseListener(new MouseAdapter()
    {
      public void mouseReleased(MouseEvent e)
      {
        if (lastClick.x >= 0 && lastClick.y >= 0)
        {
          offset.x = (int)((-(GRAPH_FULL_WIDTH - GRAPH_WIDTH) / 2) - 5);
          offset.y = (int)((-(GRAPH_FULL_HEIGHT - GRAPH_HEIGHT) / 2));
          coord.x += e.getX() - lastClick.x;
          coord.y += e.getY() - lastClick.y;
          initGraph();
          lastClick = new Point(-100, -100);
        }
      }
    });
    graphPanel.setFocusable(true);
    graphingPnl.add(graphPanel, BorderLayout.CENTER);
    graphingPnl.add(graphOptionPnl, BorderLayout.PAGE_END);
    tabPane.addTab("Graphing", graphingPnl);
    tabPane.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent event) 
      {
        if (tabPane.getSelectedIndex() == 1)
        {
          render();
          initGraph();
          graphPanel.requestFocus();
        }
      }
    });
    
    add(tabPane, BorderLayout.CENTER);
    pack();
    setVisible(true);
    
    offset.x = -200;
    offset.y = -250;
  }
  
  private void calculate(String eqn)
  {
    println(eqn);
    String answerTxt = "";
    if (eqn.toLowerCase().equals("horse"))
      println("= neigh!");
    else if (eqn.toLowerCase().equals("jeff"))
      println("= infinity + 1");
    else if (eqn.toLowerCase().equals("will"))
      println("= null");
    else if (eqn.toLowerCase().equals("burry"))
      println("= furry");
    else if (eqn.toLowerCase().equals("alex"))
      println("= 0.000000000000001");
    else if (eqn.toLowerCase().equals("neil"))
      println("= the sides of a triangle");
    else if (eqn.toLowerCase().equals("steve"))
      println("= n00b");
    else if (eqn.toLowerCase().equals("the answer to life the universe and everything"))
      println("= Jeff");
    else
    {
      history.add(eqn);
      eqn = prepareEquation(eqn);
      try
      {
        double answer = simplify(eqn);
        if (answer % (Math.PI/1000) < 0.1)
        {
          if (answer >= Math.PI)
            answerTxt = numberFormat.format(answer / Math.PI) + "*pi";
          else if ((int)(Math.PI/answer)==Math.ceil(Math.PI/answer))
            answerTxt = "pi/" + numberFormat.format(Math.PI / answer);
        }
        history.add("" + answer);
        lastAnswer = answer;
        println("= " + numberFormat.format(answer));
        if (answerTxt != "")
          println("= " + answerTxt);
        println("");
      }
      catch (Exception exception)
      {
        println("An error occurred.\n");
      }
      finally
      {
        historyIndex = history.size();
      }
    }
  }
  
  private double simplify(String eqn)
  {
    String[] terms = null;
    double[] values;
    double answer = 0;
    eqn = eqn.replaceAll("\\)\\(", "\\)*\\(");
    int n = 0;
    while (n != -1)
    {
      n = eqn.lastIndexOf("(");
      if (n != -1)
      {
        String s = eqn.substring(n+1, eqn.indexOf(")", n));
        String end = eqn.substring(eqn.indexOf(")", n)+1);
        double ans;
        if (eqn.indexOf("sqrt") != -1 && eqn.lastIndexOf("sqrt") + 4 == n)
        {
          ans = Math.sqrt(simplify(s));
          eqn = eqn.substring(0, eqn.lastIndexOf("sqrt")) + ans + end;
        }
        else if (eqn.indexOf("cbrt") != -1 && eqn.lastIndexOf("cbrt") + 4 == n)
        {
          ans = Math.cbrt(simplify(s));
          eqn = eqn.substring(0, eqn.lastIndexOf("cbrt")) + ans + end;
        }
        else if (eqn.indexOf("root") != -1 && eqn.lastIndexOf("root") + 4 == n)
        {
          String[] s2 = s.split(",");
          ans = Math.pow(simplify(s2[0]), 1/simplify(s2[1]));
          eqn = eqn.substring(0, eqn.lastIndexOf("root")) + ans + end;
        }
        else if (eqn.indexOf("abs") != -1 && eqn.lastIndexOf("abs") + 3 == n)
        {
          ans = Math.abs(simplify(s));
          eqn = eqn.substring(0, eqn.lastIndexOf("abs")) + ans + end;
        }
        else if (eqn.indexOf("asin") != -1 && eqn.lastIndexOf("asin") + 4 == n)
        {
          ans = (radianMode) ? Math.asin(simplify(s)) : Math.asin(simplify(s))*180/Math.PI;
          eqn = eqn.substring(0, eqn.lastIndexOf("asin")) + ans + end;
        }
        else if (eqn.indexOf("acos") != -1 && eqn.lastIndexOf("acos") + 4 == n)
        {
          ans = (radianMode) ? Math.acos(simplify(s)) : Math.acos(simplify(s))*180/Math.PI;
          eqn = eqn.substring(0, eqn.lastIndexOf("acos")) + ans + end;
        }
        else if (eqn.indexOf("atan") != -1 && eqn.lastIndexOf("atan") + 4 == n)
        {
          ans = (radianMode) ? Math.atan(simplify(s)) : Math.atan(simplify(s))*180/Math.PI;
          eqn = eqn.substring(0, eqn.lastIndexOf("atan")) + ans + end;
        }
        else if (eqn.indexOf("sin") != -1 && eqn.lastIndexOf("sin") + 3 == n)
        {
          ans = (radianMode) ? Math.sin(simplify(s)) : Math.sin(simplify(s)*Math.PI/180);
          eqn = eqn.substring(0, eqn.lastIndexOf("sin")) + ans + end;
        }
        else if (eqn.indexOf("cos") != -1 && eqn.lastIndexOf("cos") + 3 == n)
        {
          ans = (radianMode) ? Math.cos(simplify(s)) : Math.cos(simplify(s)*Math.PI/180);
          eqn = eqn.substring(0, eqn.lastIndexOf("cos")) + ("" + ans) + end;
        }
        else if (eqn.indexOf("tan") != -1 && eqn.lastIndexOf("tan") + 3 == n)
        {
          ans = (radianMode) ? Math.tan(simplify(s)) : Math.tan(simplify(s)*Math.PI/180);
          eqn = eqn.substring(0, eqn.lastIndexOf("tan")) + ans + end;
        }
        else if (eqn.indexOf("log") != -1 && eqn.lastIndexOf("log") + 3 == n)
        {
          ans = Math.log10(simplify(s));
          eqn = eqn.substring(0, eqn.lastIndexOf("log")) + ans + end;
        }
        else if (eqn.indexOf("ln") != -1 && eqn.lastIndexOf("ln") + 2 == n)
        {
          ans = Math.log(simplify(s));
          eqn = eqn.substring(0, eqn.lastIndexOf("ln")) + ans + end;
        }
        else if (eqn.indexOf("avg") != -1 && eqn.lastIndexOf("avg") + 3 == n)
        {
          String[] nums = s.split(",");
          double total = 0;
          for (int i = 0; i < nums.length; i++)
            total += simplify(nums[i]);
          ans = total / nums.length;
          eqn = eqn.substring(0, eqn.lastIndexOf("avg")) + ans + end;
        }
        else
        {
          ans = simplify(s);
          String st = eqn.substring(0, n);
          if (st.length() > 0 && !isOperator(st.charAt(st.length()-1)))
            eqn = st + "*" + ans + end;
          else
            eqn = st + ans + end;
        }
      }
    }
    
    eqn = eqn.replaceAll("E-", "E_");
    
    if (eqn.indexOf("+") != -1)
      terms = eqn.split("[+]");
    if (terms != null)
    {
      values = new double[terms.length];
      for (int i = 0; i < terms.length; i++)
      {
        if (terms[i].equals("") || terms[i] == null)
          values[i] = 0;
        else
        {
          values[i] = simplify(terms[i]);
          answer += values[i];
        }
      }
      return answer;
    }
    
    terms = null;
    if (eqn.indexOf("-") != -1)
      terms = eqn.split("-");
    if (terms != null)
    {
      values = new double[terms.length];
      String newEqn = "";
      boolean skip = false;
      for (int i = 0; i < terms.length; i++)
      {
        if (terms[i].equals("") || isOperator(terms[i].charAt(terms[i].length()-1)))
        {
          if (!skip && i != 0)
            newEqn += "-";
          if (i == 0)
            newEqn += terms[i] + "_";
          else
            newEqn += terms[i] + "_";
          skip = true;
        }
        else if (i == 0)
        {
          newEqn += terms[i];
        }
        else
        {
          if (!skip)
            newEqn += "-";
          newEqn += terms[i];
          skip = false;
        }
      }
      if (!newEqn.equals(""))
        eqn = newEqn;
      terms = null;
      if (eqn.indexOf("-") != -1)
        terms = eqn.split("-");
      if (terms != null)
      {
        for (int i = 0; i < terms.length; i++)
        {
          values[i] = simplify(terms[i]);
          if (i == 0)
            answer = values[i];
          else
            answer -= values[i];
        }
        return answer;
      }
    }
    eqn = eqn.replaceAll("_", "-");
    eqn = eqn.replaceAll("--", "+");
    if (eqn.charAt(0) == '+')
      eqn = eqn.substring(1, eqn.length());
    terms = null;
    if (eqn.indexOf("*") != -1)
      terms = eqn.split("[*]");
    if (terms != null)
    {
      values = new double[terms.length];
      for (int i = 0; i < terms.length; i++)
      {
        values[i] = simplify(terms[i]);
        if (i == 0)
          answer = values[i];
        else
          answer *= values[i];
      }
      return answer;
    }
    
    terms = null;
    if (eqn.indexOf("/") != -1)
      terms = eqn.split("/");
    if (terms != null)
    {
      values = new double[terms.length];
      for (int i = 0; i < terms.length; i++)
      {
        values[i] = simplify(terms[i]);
        if (i == 0)
          answer = values[i];
        else
          answer /= values[i];
      }
      return answer;
    }
    
    terms = null;
    if (eqn.indexOf("^") != -1)
      terms = eqn.split("\\^");
    if (terms != null)
    {
      values = new double[terms.length];
      for (int i = 0; i < terms.length; i++)
      {
        values[i] = simplify(terms[i]);
        if (i == 0)
          answer = values[i];
        else
          answer = Math.pow(answer, values[i]);
      }
      return answer;
    }
    return Double.parseDouble(eqn);
  }
  
  private String prepareEquation(String eqn)
  {
    eqn = eqn.replaceAll("ans", String.valueOf(lastAnswer));
    eqn = eqn.replaceAll("pi", String.valueOf(Math.PI));
    eqn = eqn.replaceAll("e", String.valueOf(Math.E));
    while (eqn.indexOf("rand") != -1)
      eqn = eqn.replaceFirst("rand", String.valueOf(Math.random()));
    eqn = eqn.replaceAll("--", "+");
    return eqn;
  }
  
  private void initGraph()
  {
    g.setFont(LOADING_FONT);
    g.setColor(Color.BLACK);
    g.drawString("Loading...", (GRAPH_FULL_WIDTH / 2) + offset.x - 50, (GRAPH_FULL_HEIGHT / 2) + offset.y);
    
    dbImage = createImage(GRAPH_FULL_WIDTH, GRAPH_FULL_HEIGHT);
    dbg = (Graphics2D) dbImage.getGraphics();
    dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    dbg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    dbg.setColor(Color.WHITE);
    dbg.fillRect(0, 0, GRAPH_FULL_WIDTH, GRAPH_FULL_HEIGHT);
    
    drawAxes();
    
    for (int i = 0; i < functions.size(); i++)
      graph(functions.get(i), colours[i % colours.length]);
    if (functions.size() == 0)
      render();
  }
  
  private void drawAxes()
  {
    dbg.setColor(Color.BLACK);
    dbg.drawLine(0, GRAPH_FULL_HEIGHT / 2 + coord.y, GRAPH_FULL_WIDTH, GRAPH_FULL_HEIGHT / 2 + coord.y);
    dbg.drawLine(GRAPH_FULL_WIDTH / 2 + coord.x, 0, GRAPH_FULL_WIDTH / 2 + coord.x, GRAPH_FULL_HEIGHT);
    
    dbg.setFont(AXIS_FONT);
    int diff = (int) (GRAPH_FULL_WIDTH / (20 * zoom.x));
    if (diff == 0)
      diff = 1;
    for (int i = diff; i < GRAPH_FULL_WIDTH; i += diff)
    {
      dbg.drawString(String.valueOf(i), (int) ((GRAPH_FULL_WIDTH / 2.0) + (i * zoom.x) + coord.x), (int) ((GRAPH_FULL_HEIGHT / 2.0) + coord.y + 10));
      dbg.drawString(String.valueOf(-i), (int) ((GRAPH_FULL_WIDTH / 2.0) - (i * zoom.x) + coord.x), (int) ((GRAPH_FULL_HEIGHT / 2.0) + coord.y + 10));
    }
    
    diff = (int) (GRAPH_FULL_HEIGHT / (20 * zoom.y));
    if (diff == 0)
      diff = 1;
    for (int i = diff; i < GRAPH_FULL_HEIGHT; i += diff)
    {
      dbg.drawString(String.valueOf(-i), (int)((GRAPH_FULL_WIDTH/2.0)+5+coord.x), (int)((GRAPH_FULL_HEIGHT/2.0)+((int)i)*zoom.y+coord.y));
      dbg.drawString(String.valueOf(i), (int)((GRAPH_FULL_WIDTH/2.0)+5+coord.x), (int)((GRAPH_FULL_HEIGHT/2.0)-((int)i)*zoom.y+coord.y));
    }
  }
  
  public double solve(String eqn, double x)
  {
    return simplify(prepareEquation(eqn.replaceAll("x", "(" + x + ")")));
  }
  
  public double solve(String eqn, double x, double t)
  {
    return simplify(prepareEquation(eqn.replaceAll("x", "(" + x + ")").replaceAll("t", "(" + t + ")")));
  }
  
  private boolean graph(String eqn, Color color)
  {
    dbg.setColor(color);
    graphPath.reset();
    
    double lastVal = Double.MAX_VALUE;
    double val;
    for (double x = -GRAPH_FULL_WIDTH / 2.0 / zoom.x - coord.x / zoom.x; x < GRAPH_FULL_WIDTH / 2.0 / zoom.x - coord.x / zoom.x; x += 0.0078125)
    {
      try
      {
        val = simplify(prepareEquation(eqn.replaceAll("x", "(" + x + ")")));
      }
      catch (Exception ex) { return false; }
      
      if (lastVal == Double.MAX_VALUE)
      {
        if (val > (GRAPH_FULL_HEIGHT/2.0)/zoom.y+coord.y/zoom.y || val < -((GRAPH_FULL_HEIGHT/2.0)/zoom.y-coord.y/zoom.y) || Double.isInfinite(val))
          continue;
        lastVal = val;
        graphPath.moveTo((x*zoom.x + GRAPH_FULL_WIDTH/2)+coord.x, (-lastVal*zoom.y + GRAPH_FULL_HEIGHT/2)+coord.y);
      }
      else if (val < (GRAPH_FULL_HEIGHT/2.0)/zoom.y+coord.y/zoom.y && val > -((GRAPH_FULL_HEIGHT/2.0)/zoom.y-coord.y/zoom.y) && !Double.isInfinite(val))
      {
        lastVal = val;
        graphPath.lineTo(x * zoom.x + GRAPH_FULL_WIDTH / 2 + coord.x, -val * zoom.y + GRAPH_FULL_HEIGHT / 2 + coord.y);
      }
      else
      {
        graphPath.lineTo(x*zoom.x + GRAPH_FULL_WIDTH/2+coord.x, -val*zoom.y + GRAPH_FULL_HEIGHT/2+coord.y);
        lastVal = Double.MAX_VALUE;
      }
    }
    dbg.draw(STROKE.createStrokedShape(graphPath));
    render();
    return true;
  }
  
  private void render()
  {
    if (g == null)
      g = (Graphics2D) graphPanel.getGraphics();
    g.drawImage(dbImage, offset.x, offset.y, GRAPH_FULL_WIDTH, GRAPH_FULL_HEIGHT, null);
  }
  
  private void println(String string)
  {
    outputTextArea.append(string + "\n");
  }
  
  private boolean isOperator(char ch)
  {
    return OPERATORS.indexOf(ch) != -1;
  }
  
  public static void main(String[] args)
  {
    new Calculator();
  }
}
