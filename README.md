# FormFlow

#### FormFlow is a powerful library for building conversational bots using the Microsoft Bot Framework. It allows developers to create complex dialog flows with ease, enabling users to interact with bots in a natural and intuitive way.

#### contextpath: /formflow port_no:8081 (eg: http://localhost:8081/formflow/)

### Swagger UI: http://localhost:8081/formflow/swagger-ui/index.html

### API endpoints:

#### backend api--
- create form -- http://localhost:8081/formflow/createForm  (post)
- get all form -- http://localhost:8081/formflow/allForm  (get)
- get form by id -- http://localhost:8081/formflow/forms/{id}  get()
- get form by status -- http://localhost:8081/formflow/forms/status/{status}

- post respose -- http://localhost:8081/formflow/submitForm  (post)
- get form response by formId -- http://localhost:8081/formflow/getResponse/{formId}  get()