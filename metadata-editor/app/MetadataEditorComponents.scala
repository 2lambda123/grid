import com.gu.mediaservice.lib.imaging.ImageOperations
import com.gu.mediaservice.lib.play.GridComponents
import controllers.{EditsApi, EditsController}
import lib._
import play.api.ApplicationLoader.Context
import router.Routes

class MetadataEditorComponents(context: Context) extends GridComponents(context) {
  final override lazy val config = new EditsConfig(configuration)

  val store = new EditsStore(config)
  val notifications = new Notifications(config)
  val imageOperations = new ImageOperations(application.path.getAbsolutePath)

  val editsController = new EditsController(auth, store, notifications, config, controllerComponents)
  val controller = new EditsApi(auth, config, controllerComponents)

  override lazy val router = new Routes(httpErrorHandler, controller, editsController, management)
}
