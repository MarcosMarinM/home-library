# app.R
library(shiny)
library(bslib)
library(dplyr)
library(stringr)
library(stringi)
library(DT)
library(googlesheets4) # Package to connect to Google Sheets
library(shinymanager)  # Package for the login and password screen

# ---------------------------------------------
# CONFIGURATION
# ---------------------------------------------
# We tell R NOT to request Google account permissions
gs4_deauth()

# Load passwords and the spreadsheet ID from the hidden file
source("passwords.R")

# ---------------------------------------------
# Helpers (Helper functions)
# ---------------------------------------------
alert_box <- function(type = c("info","success","warning","danger"), ...) {
  type <- match.arg(type)
  tags$div(class = paste0("alert alert-", type, " mb-3"), role = "alert", ...)
}

# Spanish text for the table
dt_es <- list(
  sInfo         = "_START_ a _END_ de _TOTAL_ libros",
  sInfoEmpty    = "0 libros",
  sZeroRecords  = "No se encontraron libros",
  oPaginate = list(
    sPrevious = "←",
    sNext     = "→"
  )
)

# Function to clean text (removes accents, uppercase, etc.)
normalizar <- function(x) {
  x <- as.character(x)
  x <- str_replace_all(x, "\u00A0", " ")
  x <- str_squish(x)
  x <- str_to_lower(x)
  x <- stringi::stri_trans_general(x, "Latin-ASCII")     
  x <- str_replace_all(x, "[^a-z0-9 ]+", " ")
  x <- str_squish(x)
  x
}

# Function to interpret where the book is, now with the new options
interpretar_profundidad <- function(x) {
  xx <- normalizar(x)
  dplyr::case_when(
    xx %in% c("f", "frente", "delante") ~ "Delante",
    xx %in% c("t", "tras", "detras", "detrás") ~ "Detrás",
    xx %in% c("b", "debajo") ~ "Debajo",       # New option B
    xx %in% c("a", "encima") ~ "Encima",       # New option A
    xx %in% c("c", "completa") ~ "Completa",   # New option C
    TRUE ~ as.character(x)
  )
}

# Read the main database from the "libros" tab in the spreadsheet
leer_biblioteca <- function(id_hoja) {
  # We read the "libros" tab. 
  # read_sheet takes row 1 as the header by default, which is exactly what you did.
  df <- read_sheet(
    ss = id_hoja,
    sheet = "libros",
    col_types = "c" # We read everything as text for safety to avoid formatting errors
  )
  
  # Ensure we only have the 7 columns we care about
  df <- df[, 1:7]
  colnames(df) <- c("Título","Autor","Color_Lomo","Balda","Cuadrante","Profundidad","Posicion_Relativa")
  
  df %>%
    mutate(across(everything(), ~ str_squish(as.character(.)))) %>%
    mutate(
      Balda = suppressWarnings(as.integer(Balda)),
      Posicion_Relativa = suppressWarnings(as.integer(Posicion_Relativa)),
      Cuadrante = as.character(Cuadrante),
      Profundidad = interpretar_profundidad(Profundidad),
      # Add the new levels to the logical order of depths
      Profundidad_orden = factor(Profundidad, levels = c("Delante", "Detrás", "Encima", "Debajo", "Completa")),
      Título_norm = normalizar(Título),
      Autor_norm  = normalizar(Autor),
      Color_norm  = normalizar(Color_Lomo)
    ) %>%
    # Order the table logically
    arrange(Balda, Cuadrante, Profundidad_orden, Posicion_Relativa, Título)
}

# Read the colour dictionary from the "colores" tab
leer_colores <- function(id_hoja) {
  # Since you pasted the colours txt, we assume it has NO header, hence col_names = FALSE
  df <- read_sheet(
    ss = id_hoja,
    sheet = "colores",
    col_names = FALSE,
    col_types = "c"
  )
  
  if (ncol(df) < 2) return(c())
  
  # Links column 2 (HEX code) with column 1 (colour name)
  setNames(str_squish(df[[2]]), str_to_lower(str_squish(df[[1]])))
}

# Draws the little colour square in the table
generar_html_colores <- function(color_str, dicc) {
  if (is.na(color_str) || color_str == "") return(color_str)
  if (length(dicc) == 0) return(color_str)
  
  partes <- str_split(color_str, ",|\\by\\b")[[1]]
  partes <- str_squish(partes)
  
  bloques <- sapply(partes, function(p) {
    p_low <- str_to_lower(p)
    hex <- dicc[p_low]
    if (!is.na(hex)) {
      paste0('<span style="display:inline-block; width:12px; height:18px; background-color:', hex, 
             '; border-radius:3px; margin-right:4px; border:1px solid #cbd5e1; flex-shrink:0;"></span>')
    } else {
      ""
    }
  }, USE.NAMES = FALSE)
  
  bloques_juntos <- paste(bloques, collapse = "")
  
  if (bloques_juntos != "") {
    return(paste0('<div style="display:flex; align-items:center;">', bloques_juntos, '<span>', color_str, '</span></div>'))
  } else {
    return(color_str)
  }
}

# ---------------------------------------------
# VISUAL THEME
# ---------------------------------------------
tema <- bs_theme(
  version = 5,
  bootswatch = "zephyr",
  base_font = font_google("Inter"),
  primary = "#2563EB"
)

# ---------------------------------------------
# LOGIN TEXT CUSTOMISATION
# ---------------------------------------------
set_labels(
  language = "es",
  "Please authenticate" = "📚 Acceso a la biblioteca",
  "Username:" = "Usuario:",
  "Password:" = "Contraseña:",
  "Login" = "Entrar"
)

# ---------------------------------------------
# USER INTERFACE (UI)
# ---------------------------------------------
# Wrap our original UI in secure_app() to insert the login screen
ui <- secure_app(
  page_fluid( 
    theme = tema,
    title = "Biblioteca",
    
    tags$head(
      tags$meta(name = "viewport", content = "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"),
      tags$style(HTML("
        body { background: #F6F7FB; padding-top: 15px;}
        .card { border: 0; box-shadow: 0 4px 12px rgba(15, 23, 42, .05); border-radius: 12px; margin-bottom: 15px; }
        .app-sub { color: rgba(0,0,0,.55); font-size: 0.9rem; }
        
        #q { font-size: 1.1rem; padding: 12px; padding-right: 40px !important; border-radius: 8px;} 
        .form-group.shiny-input-container { margin-bottom: 0 !important; }
        
        /* Colour of the native DataTables arrow */
        table.dataTable.dtr-inline.collapsed>tbody>tr>td.dtr-control:before, 
        table.dataTable.dtr-inline.collapsed>tbody>tr>th.dtr-control:before,
        table.dataTable.dtr-column>tbody>tr>td.dtr-control:before, 
        table.dataTable.dtr-column>tbody>tr>th.dtr-control:before {
          background-color: #2563EB !important;
        }

        /* Make the whole row show it's 'clickable' */
        table.dataTable tbody tr { cursor: pointer; }
        
        /* Occupy the full width and reset base styles */
        table.dataTable>tbody>tr.child ul.dtr-details { width: 100%; display: block; }
        
        /* Convert each line of the dropdown into an aligned Flex box */
        table.dataTable>tbody>tr.child ul.dtr-details>li { 
          display: flex !important; align-items: center; padding: 10px 0 !important; border-bottom: 1px solid #e2e8f0 !important; 
        }
        table.dataTable>tbody>tr.child ul.dtr-details>li:last-child { border-bottom: none !important; }
        
        /* Fixed width and bold for the titles (Shelf, Quadrant...) */
        table.dataTable>tbody>tr.child span.dtr-title { 
          min-width: 110px !important; font-weight: 600 !important; color: #475569; margin-right: 10px;
        }
        
        /* The data occupies the rest of the free space */
        table.dataTable>tbody>tr.child span.dtr-data { flex-grow: 1 !important; color: #0f172a; }
      "))
    ),
    
    tags$div(
      class = "text-center mb-3",
      tags$h2("📚 Biblioteca", style = "margin-bottom:.25rem; font-weight: 600; color: #1E293B;"),
      tags$div(class = "app-sub", "Encuentra cualquier libro al instante")
    ),
    
    card(
      card_body(
        tags$div(
          style = "position: relative;",
          textInput("q", NULL, width = "100%", placeholder = "🔍 Buscar título, autor, color..."),
          tags$button(
            id = "btn_clear",
            class = "action-button",
            style = "position: absolute; right: 12px; top: 50%; transform: translateY(-50%); border: none; background: transparent; color: #94a3b8; font-size: 1.2rem; padding: 0; z-index: 10;",
            "✖"
          )
        ),
        actionButton("btn_lista", "📃 Ver catálogo completo", class = "btn btn-light w-100", style="margin-top: 10px;")
      )
    ),
    
    card(
      card_body(
        uiOutput("estado"),
        DTOutput("tabla_busqueda")
      )
    )
  ),
  # Set the login interface to Spanish
  language = "es",
  theme = bs_theme(version = 5, primary = "#2563EB")
)

# ---------------------------------------------
# SERVER
# ---------------------------------------------
server <- function(input, output, session) {
  
  # 1. THIS ACTIVATES SECURITY. It only loads the rest if you enter the password.
  res_auth <- secure_server(
    check_credentials = check_credentials(credenciales)
  )
  
  datos <- reactiveVal(NULL)
  dicc_colores <- reactiveVal(c())
  
  # Load data from Google Sheets on startup (if login is passed)
  observe({
    req(res_auth) # Ensures we are logged in
    tryCatch({
      datos(leer_biblioteca(HOJA_GOOGLE))
      dicc_colores(leer_colores(HOJA_GOOGLE))
    }, error = function(e) {
      datos(NULL)
      showNotification(paste("Error al leer Google Sheets:", e$message), type = "error", duration = 8)
    })
  })
  
  # Clear search
  observeEvent(input$btn_clear, {
    updateTextInput(session, "q", value = "")
  })
  
  # Filtering logic
  filtrado <- reactive({
    df <- datos()
    validate(need(!is.null(df), "Cargando biblioteca desde Google Sheets..."))
    
    q <- str_squish(input$q)
    if (is.null(q) || q == "") return(df[0, ]) 
    
    qn <- normalizar(q)
    
    df %>% filter(
      str_detect(Título_norm, fixed(qn)) | 
        str_detect(Autor_norm, fixed(qn)) | 
        str_detect(Color_norm, fixed(qn))
    )
  })
  
  # Top message for results
  output$estado <- renderUI({
    df_all <- datos()
    if (is.null(df_all)) return(alert_box("danger", tags$strong("No se han podido cargar los datos de Google.")))
    
    q <- str_squish(input$q)
    if (is.null(q) || q == "") return(NULL) 
    
    df <- filtrado()
    
    if (nrow(df) == 0) return(alert_box("warning", tags$strong("No he encontrado ningún libro con esos datos.")))
    
    alert_box("info", tags$strong(paste0("He encontrado ", nrow(df), " libros.")))
  })
  
  # Search table
  output$tabla_busqueda <- renderDT({
    df <- filtrado()
    if(nrow(df) == 0) return(NULL)
    
    diccionario <- dicc_colores()
    
    df_mostrar <- df %>% 
      mutate(Color_Visual = sapply(Color_Lomo, generar_html_colores, dicc = diccionario)) %>%
      select(
        Título, 
        Autor, 
        Color = Color_Visual, 
        Balda, 
        Cuadrante, 
        Profundidad, 
        Posición = Posicion_Relativa
      )
    
    datatable(
      df_mostrar,
      rownames = FALSE,
      extensions = 'Responsive', 
      escape = FALSE,
      options = list(
        dom = 'tp', 
        pageLength = 10,
        language = dt_es,
        responsive = list(
          details = list(
            type = 'column',
            target = 'tr'
          )
        ), 
        columnDefs = list(
          list(className = 'dtr-control', targets = 0),
          list(className = 'none', targets = c(3, 4, 5, 6))
        )
      ),
      selection = 'single' 
    )
  })
  
  # Modal to view full catalogue
  observeEvent(input$btn_lista, {
    df <- datos()
    if (is.null(df)) return()
    
    showModal(
      modalDialog(
        title = "📃 Catálogo completo",
        size = "l",
        easyClose = TRUE,
        footer = modalButton("Cerrar"),
        DTOutput("tabla_total_modal")
      )
    )
  })
  
  # Full table inside the Modal
  output$tabla_total_modal <- renderDT({
    diccionario <- dicc_colores()
    
    df <- datos() %>% 
      mutate(Color_Visual = sapply(Color_Lomo, generar_html_colores, dicc = diccionario)) %>%
      select(
        Título, 
        Autor, 
        Color = Color_Visual, 
        Balda, 
        Cuadrante, 
        Profundidad, 
        Posición = Posicion_Relativa
      )
    
    datatable(
      df,
      rownames = FALSE,
      extensions = 'Responsive',
      escape = FALSE,
      options = list(
        dom = 'tp',
        pageLength = 15,
        language = dt_es,
        responsive = list(
          details = list(
            type = 'column',
            target = 'tr'
          )
        ),
        columnDefs = list(
          list(className = 'dtr-control', targets = 0),
          list(className = 'none', targets = c(3, 4, 5, 6))
        )
      ),
      selection = 'single'
    )
  })
}

shinyApp(ui, server)