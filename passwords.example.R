# Example configuration file for the Shiny app.
#
# Copy this file to `passwords.R` (which is ignored by Git) and fill in the real values.

# Google Sheets ID
# Example: https://docs.google.com/spreadsheets/d/1Wf...XYZ/edit#gid=0
HOJA_GOOGLE <- "REEMPLAZA_POR_EL_ID_DE_TU_HOJA"

# User and password (the Shiny app uses these for a simple login screen)
credenciales <- data.frame(
  user = c("libros"),
  password = c("libros"),
  stringsAsFactors = FALSE
)
