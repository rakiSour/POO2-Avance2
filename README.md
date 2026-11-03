# Bembos · Sistema de Gestión de Pedidos (MVC + Builder)

Proyecto del curso **Programación Orientada a Objetos II** — Universidad San Ignacio de Loyola (USIL), 2026.
Autor: Dylan Mijail Fuentes Cerón · Docente: Héctor Odín Delgado Enríquez.

Aplicación de escritorio en **Java 24 + Swing** con arquitectura **MVC**, persistencia en **PostgreSQL** (administrado con pgAdmin), construida con **Maven** y probada con **JUnit 5**.

**Demo web:** la misma aplicación Swing se ejecuta en el navegador (modo memoria) gracias a
[CheerpJ](https://cheerpj.com), publicada como sitio estático en Vercel → `https://<tu-proyecto>.vercel.app`

## Patrones de diseño implementados

| Patrón | Tipo | Clase(s) | Propósito en el sistema |
|---|---|---|---|
| **MVC** | Arquitectónico | `model.*`, `view.*`, `controller.*` | Separar datos/reglas, interfaz Swing y orquestación |
| **Builder** | Creacional | `ItemPedidoBuilder`, `PedidoBuilder`, `PedidoDirector` | Ensamblar paso a paso pedidos ultra personalizables y validarlos antes de crearlos |
| **Factory** | Creacional | `ProductoFactory`, `PasarelaPagoFactory`, `DAOFactory` (Abstract Factory) | Crear productos, pasarelas de pago y familias de DAO sin acoplarse a clases concretas |
| **Singleton** | Creacional | `config.ConexionBD` | Único punto de acceso a PostgreSQL con pool de conexiones |
| **Adapter** | Estructural | `FacturacionLegacyAdapter` → `SistemaFacturacionLegacy` | Integrar el módulo de facturación heredado (tramas de texto) |
| **Facade** | Estructural | `PedidoFacade` | Interfaz única para los controladores: registrar, `procesarPago()`, cocina, reportes |

## Estructura

```
src/main/java/org/usil/bembos
├── App.java                      ← punto de entrada (ensambla MVC)
├── config/ConexionBD.java        ← Singleton + pool
├── model/
│   ├── entidad/                  ← Producto, Hamburguesa, ItemPedido, Pedido, EstadoPedido...
│   ├── builder/                  ← ItemPedidoBuilder, PedidoBuilder, PedidoDirector
│   ├── factory/ProductoFactory
│   ├── dao/                      ← interfaces + jdbc/ (PostgreSQL) + memoria/ (pruebas/demo)
│   ├── servicio/PedidoFacade
│   ├── servicio/pago/            ← PasarelaPago + PasarelaPagoFactory
│   └── servicio/facturacion/     ← ServicioFacturacion, SistemaFacturacionLegacy, Adapter
├── controller/                   ← PedidoController, PagoController, CocinaController, ReporteController
└── view/                         ← MainView, PedidoPanel, PersonalizacionDialog, PagoDialog, CocinaPanel, ReportePanel
src/main/resources
├── db.properties                 ← URL, usuario y clave de PostgreSQL
└── db/01_schema.sql, db/02_datos.sql
src/test/java/...                 ← 62 pruebas JUnit 5 (CP-01 … CP-48)
docs/                             ← diagramas UML (.png, .puml), modelo StarUML (.mdj), informe y capturas
web/                              ← demo web: index.html (CheerpJ) + bembos-web.jar (lo publica Vercel)
vercel.json                       ← Vercel sirve la carpeta web/ sin compilar nada
.github/workflows/                ← CI (pruebas con PostgreSQL) y Release (JAR descargable)
```

## Puesta en marcha

1. **Base de datos (pgAdmin)**
   1. Crear la base: clic derecho en *Databases → Create → Database…* con nombre `bembos_db`
      (o en Query Tool: `CREATE DATABASE bembos_db ENCODING 'UTF8';`).
   2. Seleccionar `bembos_db` → *Query Tool* → abrir y ejecutar `src/main/resources/db/01_schema.sql`.
   3. En la misma ventana, abrir y ejecutar `src/main/resources/db/02_datos.sql`.
   4. Editar `src/main/resources/db.properties` con su usuario y clave de PostgreSQL.
2. **Ejecutar la aplicación**
   - IntelliJ: ejecutar `org.usil.bembos.App` (o la clase `Main`, que delega en `App`).
   - Maven: `mvn compile exec:java`
   - JAR: `mvn package` y luego `java -jar target/POO2-Avance2-1.0-SNAPSHOT.jar`
   - Sin PostgreSQL: la app ofrece un **modo demostración** en memoria, o usar el argumento `--memoria`.
3. **Pruebas**: `mvn test`. Las pruebas de integración y estrés contra PostgreSQL se ejecutan solo si la base
   `bembos_db` está disponible; de lo contrario se omiten (skipped) y el resto corre en memoria.
   Las métricas de rendimiento se guardan en `target/metricas/`.

> El esquema de la base es re-ejecutable: `01_schema.sql` elimina y vuelve a crear el esquema `bembos`.

## Demo web en el navegador (Vercel + CheerpJ)

Vercel no ejecuta Java, pero sí publica sitios estáticos. La carpeta `web/` contiene una página que
carga **CheerpJ** (una máquina virtual Java en WebAssembly) y ejecuta el JAR de la aplicación Swing
con el argumento `--web`, que activa:

- datos **en memoria** (no requiere PostgreSQL; los pedidos se pierden al recargar),
- ventana maximizada dentro de la página.

El JAR web se compila con Java 17 (versión estable en CheerpJ) y sin el driver de PostgreSQL.
Para regenerarlo después de cambiar el código:

```bash
mvn -Pweb package          # crea web/bembos-web.jar
```

Para probar la página localmente se necesita un servidor con soporte de *HTTP Range requests*
(no sirve abrir el archivo con doble clic):

```bash
npx serve web              # luego abrir http://localhost:3000
```

> CheerpJ es gratuito para proyectos personales y académicos de individuos (licencia Community).

## Publicar en GitHub

1. Crear un repositorio **vacío** en https://github.com/new (sin README ni .gitignore), por ejemplo `POO2-Avance2`.
2. En una terminal, dentro de la carpeta del proyecto:

```bash
git init -b main
git add .
git commit -m "Avance 2: sistema MVC con patrón Builder, pruebas JUnit y demo web"
git remote add origin https://github.com/<tu-usuario>/POO2-Avance2.git
git push -u origin main
```

Al subir, GitHub Actions ejecuta automáticamente el flujo **CI**: levanta PostgreSQL 16, crea el esquema
`bembos`, corre las 62 pruebas y verifica el build web. Para publicar un JAR descargable de escritorio:

```bash
git tag v2.0
git push origin v2.0       # el flujo Release adjunta los JAR y scripts SQL
```

## Desplegar en Vercel

1. Entrar a https://vercel.com con la cuenta de GitHub.
2. **Add New → Project → Import** el repositorio `POO2-Avance2`.
3. Dejar la configuración que detecta (`vercel.json` ya indica *Framework: Other* y *Output Directory: web*) y pulsar **Deploy**.
4. Cada `git push` a `main` vuelve a desplegar la demo automáticamente.

Si después de cambiar el código la demo no refleja los cambios, regenerar `web/bembos-web.jar`
con `mvn -Pweb package`, hacer commit y push.
