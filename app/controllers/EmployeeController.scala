package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import services.EmployeeDao
import services.ResumeDao
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future

import play.modules.reactivemongo.json._, ImplicitBSONHandlers._
import reactivemongo.bson._


object EmployeeController extends Controller {

   implicit val resumeWrites = new Writes[Resume] {
    def writes(r: Resume): JsValue = {
      Json.obj(
        "id" -> r._id.stringify,
        "creator_id" -> r.creator_id.stringify,
        "name" -> r.name,
        "description" -> r.description,
        "skills" -> r.skills,
        "experience" -> r.experience,
        "education" -> r.education,
        "salary" -> r.salary
      )
    }
  }

  case class NewEmployeeForm(
    name: String,
    category: String,
    status: String,
    email: String,
    phone: String) {
    def toEmployee: Employee = Employee(BSONObjectID.generate, name, category, 
      status, email, phone)
  }
//TODO add List[BSONObjectID]
  case class EditEmployeeForm(
    id: String,
    name: String,
    category: String,
    status: String,
    email: String,
    phone: String
    ) {
    def toEmployee: Employee = Employee(new BSONObjectID(id), name, category, 
      status, email, phone)
  }

  implicit val newEmployeeFormFormat = Json.format[NewEmployeeForm]
  implicit val editEmployeeFormFormat = Json.format[EditEmployeeForm]

  def getEmployees() = Action.async { implicit req =>
    for {
      employees <- EmployeeDao.findAll()
    } yield {
      println(employees)
      Ok(Json.toJson(employees))
    }
  }

  def deleteById() = Action.async(parse.json) { implicit req =>
    (req.body \ "id").asOpt[String].map { id => 
      for{
        id <- EmployeeDao.deleteById(new BSONObjectID(id.toString))
        } yield {
          println(id)
          Ok("Delete!!!")
        }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def getById() = Action.async(parse.json) { implicit req =>
    (req.body \ "id").asOpt[String].map { id =>
      for {
        employee <- EmployeeDao.findById(new BSONObjectID(id.toString))
      } yield {
        println(employee)
        Ok(Json.toJson(employee))
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def saveEmployee = Action.async(parse.json) { req =>
    println(req.body)
    Json.fromJson[NewEmployeeForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad resume form")),
      form => EmployeeDao.save(form.toEmployee).map(_ => Created)
    )
  }

  def updateEmployee = Action.async(parse.json) { req =>
    println(req.body)
    Json.fromJson[EditEmployeeForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad resume form")),
      form => EmployeeDao.save(form.toEmployee).map(_ => Created)
    )
  }

  def getResumes() = Action.async(parse.json) { implicit req =>
    (req.body \ "id").asOpt[String].map { id => 
      for{
        resumeList <- ResumeDao.findByCreatorId(new BSONObjectID(id.toString))
      } yield {
        println(resumeList)
        Ok(Json.obj("resumes" -> Json.toJson(resumeList),
          "error_code" -> 0))
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

}