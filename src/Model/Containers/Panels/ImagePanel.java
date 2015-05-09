package Model.Containers.Panels;

import Gui.ImageStorage;
import Model.*;
import Model.Containers.ResizableScroll;
import Model.Containers.Storyspace;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

public class ImagePanel extends JPanel implements IModel {

	private BufferedImage image = null;
	private JLabel imageLabel = null;
	private String imagePath = "";

	private Storyspace parentStoryspace = null;
	private AbstractHandler handler = null;

	public ImagePanel(Storyspace parentStoryspace) {
		setFocusable(true);
		this.add(imageLabel = new JLabel("Image not loaded"));

		handler = new AbstractHandler(this) {
			@Override
			protected void initActionMap() {
				addCombo(ctrl, k.VK_O).setDo(makeOpenFileDialog(getContext()::loadImage));
			}
			@Override
			public Boolean mousePressedFinal(ComboMouse mouse) {
				if (mouse.leftButton) {
					getContext().requestFocus();
					return true;
				} else { return false; }
			}
			@Override
			public ImagePanel getContext() {
				return (ImagePanel) super.getContext();
			}
		};
		this.addKeyListener(handler);
		this.addMouseListener(handler);
		this.addMouseMotionListener(handler);

		(this.parentStoryspace = parentStoryspace).addModelChild(this);
	}

	@Override
	public AbstractModel getFocusedChild() { return null; }
	@Override
	public ResizableScroll getModelParent() { return ResizableScroll.class.cast(getParent().getParent()); } // =D
	@Override
	public AbstractHandler getHandler() { return this.handler; }

	@Override
	public JSONObject getJsonRepresentation() {
		JSONObject dict = new JSONObject();
		dict.put("className", getClass().getSimpleName());
		dict.put("imagePath", this.imagePath);
		return dict;
	}
	@Override
	public ImagePanel reconstructFromJson(JSONObject jsObject) throws JSONException {
		this.loadImage(new File(jsObject.getString("imagePath")));
		return this;
	}

	// event handles

	private void loadImage(File file) {
		this.imagePath = file.getAbsolutePath();
		this.image = ImageStorage.inst().openImage(file);

		this.remove(imageLabel);
		this.add(imageLabel = new JLabel(new ImageIcon(image)));
		this.validate();
	}

	final private Consumer<Combo> makeOpenFileDialog(Consumer<File> lambda) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Image Files", ImageIO.getReaderFileSuffixes()));
		return combo -> {
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				lambda.accept(chooser.getSelectedFile());
			}
		};
	}
}