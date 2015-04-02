
package Model;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractModel implements IModel {

	private IModel parent = null;
	private AbstractHandler eventHandler = null;

	public AbstractModel(IModel parent) { // TODO: parent should be AbstractModel
		this.parent = parent;
		this.eventHandler = this.makeHandler();
	}

	abstract public JSONObject getJsonRepresentation();
	abstract public IModel reconstructFromJson(JSONObject jsObject) throws JSONException;

	abstract public List<? extends AbstractModel> getChildList();
	abstract public AbstractModel getFocusedChild();

	abstract protected AbstractHandler makeHandler();
	final public AbstractHandler gettHandler() { return this.eventHandler; }

	// seems, it won't be needed her, better to use it in AbstractHandler
	public Boolean undo() {
		Boolean completedInChild = false;
		if (this.getFocusedChild() != null) {
			completedInChild = this.getFocusedChild().undo();
		}
		
		return completedInChild ? true : this.undoFinal();
	}
	public Boolean redo() {
		Boolean completedInChild = false;
		if (this.getFocusedChild() != null) {
			completedInChild = this.getFocusedChild().redo();
		}
		
		return completedInChild ? true : this.redoFinal();
	}
	abstract protected Boolean undoFinal();
	abstract protected Boolean redoFinal();

	public abstract void drawOn(Graphics surface, int x, int y);

	// field getters
	
	public IModel getParent() {
		return this.parent;
	}
}
