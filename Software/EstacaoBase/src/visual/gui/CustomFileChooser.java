/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Seletor de arquivos com checagem de se o arquivo já existe.
 * Adiciona extensão automaticamente ao arquivo.
 * Ver:
 * http://stackoverflow.com/questions/8581215/jfilechooser-and-checking-for-overwrite
 * @author stefan
 */
public class CustomFileChooser extends JFileChooser {
  private String extension;
  public CustomFileChooser(String path, String extension) {
    super(path);
    this.extension = extension;
    addChoosableFileFilter(new FileNameExtensionFilter(
      String.format("%1$s files (*.%1$s)", extension), extension));
  }

  @Override public File getSelectedFile() {
    File selectedFile = super.getSelectedFile();

    if (selectedFile != null) {
      String name = selectedFile.getName();
      if (!name.contains("."))
        selectedFile = new File(selectedFile.getParentFile(), 
          name + '.' + extension);
    }

    return selectedFile;
  }

  @Override public void approveSelection() {
    if (getDialogType() == SAVE_DIALOG) {
      File selectedFile = getSelectedFile();
      if ((selectedFile != null) && selectedFile.exists()) {
        int response = JOptionPane.showConfirmDialog(this,
          "O arquivo \"" + selectedFile.getName() + 
          "\" já existe. Você deseja substituí-lo?",
          "Sobrescrever arquivo", JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE);
        if (response != JOptionPane.YES_OPTION)
          return;
      }
    }

    super.approveSelection();
  }
}
