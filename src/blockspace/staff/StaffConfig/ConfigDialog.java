package blockspace.staff.StaffConfig;


import gui.ImageStorage;
import model.AbstractModel;
import model.field.Arr;
import model.field.Field;
import stuff.OverridingDefaultClasses.ModelFieldInput;
import stuff.OverridingDefaultClasses.TruLabel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigDialog extends JPanel {

	final private static int CELL_HEIGHT = 30;
	final private static int CELL_WIDTH = 75;

	List<ModelFieldInput> inputList = new ArrayList<>();
	Consumer<AbstractModel> onConfirm;

	StaffConfig parent = null;

	public ConfigDialog(StaffConfig parent) {
		super();
		this.parent = parent;

		this.addChannelSetupGrid();
		this.addPropertyGrid();
	}

	public void showMenuDialog(Consumer<AbstractModel> onConfirm) {
		int option = JOptionPane.showConfirmDialog(null, this, "Enter instruments for channels", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			confirmChanges();
			onConfirm.accept(parent);
		}
	}

	private void addChannelSetupGrid() {

		List<String> fieldList = parent.channelList.get(0).getFieldList();

		JPanel channelGridPanel = new JPanel(new GridLayout(Channel.CHANNEL_COUNT + 1, fieldList.size(), 4, 4));
		channelGridPanel.setPreferredSize(new Dimension(fieldList.size() * CELL_WIDTH, (Channel.CHANNEL_COUNT + 1) * CELL_HEIGHT));
		channelGridPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.DARK_GRAY));
		this.add(channelGridPanel);

		// header grid row
		for (String header: fieldList) { channelGridPanel.add(new TruLabel(header)); }

		parent.getModelHelper().getFieldStorage().stream().filter(field -> field instanceof Arr).forEach(field -> {
			Arr arr = (Arr)field;
			for (int i = 0; i < arr.size(); ++i) {

				AbstractModel model = arr.get(i);
				for (Field channelField: model.getModelHelper().getFieldStorage()) {
					JTextField textField = checkEm(new ModelFieldInput(channelField));
					textField.setForeground(ImageStorage.getColorByChannel(i));
					channelGridPanel.add(textField);
				}
			}
		});
	}

	private void addPropertyGrid() {

		List<Field> propertyList = parent.getModelHelper().getFieldStorage().stream().filter(field -> !(field instanceof Arr)).collect(Collectors.toList());

		JPanel propertyGridPanel = new JPanel(new GridLayout(propertyList.size() + 1, 2, 4, 4));
		propertyGridPanel.setPreferredSize(new Dimension(2 * CELL_WIDTH, (propertyList.size() + 1) * CELL_HEIGHT));
		this.add(propertyGridPanel);

		String[] gridHeaders = new String[]{"Prop.", "Value"};
		// header grid row
		for (String header: gridHeaders) { propertyGridPanel.add(new TruLabel(header)); }

		// filling grid with cells
		for (Field field: propertyList) {
			propertyGridPanel.add(new TruLabel(field.getName()));
			propertyGridPanel.add(checkEm(new ModelFieldInput(field)));
		}
	}

	private void confirmChanges() {
		for (ModelFieldInput input: inputList) {
			if (!input.getOwner().isFinal) {
				input.getOwner().setValueFromString(input.getText());
			}
		}
	}

	private ModelFieldInput checkEm(ModelFieldInput input) {
		this.inputList.add(input);
		return input;
	}
}
