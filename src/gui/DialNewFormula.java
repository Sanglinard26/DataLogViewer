package gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

import log.Formula;

public final class DialNewFormula extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField txtName;
	private final JTextField txtUnit;
	private final JTextArea formulaText;

	public DialNewFormula(final Ihm ihm) {

		super(ihm, "Edition de formule", false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final GridBagConstraints gbc = new GridBagConstraints();

		setLayout(new GridBagLayout());

		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(10, 5, 0, 0);
		gbc.anchor = GridBagConstraints.CENTER;
		add(new JLabel("Nom :"), gbc);

		txtName = new JTextField(20);
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(0, 0, 0, 5);
		gbc.anchor = GridBagConstraints.CENTER;
		add(txtName, gbc);

		gbc.fill = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.anchor = GridBagConstraints.CENTER;
		add(new JLabel("Unite :"), gbc);

		txtUnit = new JTextField(20);
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(0, 0, 0, 5);
		gbc.anchor = GridBagConstraints.CENTER;
		add(txtUnit, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.anchor = GridBagConstraints.CENTER;
		formulaText = new JTextArea(5, 60);
		formulaText.setLineWrap(true);
		formulaText.setWrapStyleWord(true);
		formulaText.setTransferHandler(new TransferHandler("measure")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferSupport support) {
				return true;
			}

			@Override
			public boolean importData(TransferSupport supp) {
				// Fetch the Transferable and its data
				Transferable t = supp.getTransferable();
				String data = "";
				try {
					data = (String) t.getTransferData(DataFlavor.stringFlavor);
					
					if(!data.equals(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor))) //vérification si ça vient du presse-papier
					{
						data = "<" + data + ">";
					}

				} catch (UnsupportedFlavorException e) {
					if(!"".equals(data))
					{
						data = "<" + data + ">";
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				formulaText.insert(data, formulaText.getCaretPosition());

				return true;
			}
		});
		add(formulaText, gbc);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 0, 5);
		gbc.anchor = GridBagConstraints.CENTER;
		add(createPad(), gbc);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 0, 5);
		gbc.anchor = GridBagConstraints.CENTER;
		add(new JButton("Annuler"), gbc);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(0, 5, 10, 5);
		gbc.anchor = GridBagConstraints.CENTER;
		add(new JButton(new AbstractAction("Valider") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Formula formula = new Formula(txtName.getText(), formulaText.getText(), ihm.getLog());
				if(formula.isValid())
				{
					ihm.addMeasure(formula);
					dispose();
				}
			}
		}), gbc);

		pack();
		setLocationRelativeTo(null);
		setResizable(true);
		setVisible(true);
	}


	private final JPanel createPad()
	{
		String[] tab_string = new String[]{"^", "sqrt()", "cos()", "sin()", "tan()", "+","-", "*", "/", ".", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
		JButton[] tab_button = new JButton[tab_string.length];

		JPanel pad = new JPanel(new GridLayout(4, 5));

		for(int i = 0; i < tab_string.length; i++){
			tab_button[i] = new JButton(tab_string[i]);
			tab_button[i].addActionListener(new PadListener());
			pad.add(tab_button[i]);
		}

		return pad;
	}

	private final class PadListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			//On affiche le chiffre additionnel dans le label
			String str = ((JButton)e.getSource()).getText();
			formulaText.insert(str, formulaText.getCaretPosition());

		}

	}

}
