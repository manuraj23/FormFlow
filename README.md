# FormFlow

#### FormFlow is a powerful library for building conversational bots using the Microsoft Bot Framework. It allows developers to create complex dialog flows with ease, enabling users to interact with bots in a natural and intuitive way.

#### contextpath: /formflow port_no:8082 (eg: http://localhost:8082/formflow/)

### Swagger UI: http://localhost:8082/formflow/swagger-ui/index.html

## API endpoints:

### Authentication APIs:
- login -- http://localhost:8082/formflow/auth/login  (post)
- register -- http://localhost:8082/formflow/auth/register  (post)
- logut -- http://localhost:8082/formflow/auth/logout  (post)
- refresh Token -- http://localhost:8082/formflow/auth/refresh  (post)

### User APIs--  
#### (get Access Token and Resfresh Token from login API and use it in header for below APIs)
- create form -- http://localhost:8082/formflow/user/createForm  (post)
- get all form -- http://localhost:8082/formflow/user/allForm  (get)
- get form by id -- http://localhost:8082/formflow/forms/{id}  get() id is form-id
- get form by status -- http://localhost:8082/formflow/user/status/{status}  status is form status (eg: published, draft)
- update form -- http://localhost:8082/formflow/user/updateForm/{id}  (put) id is form id
- moveToTrash form -- http://localhost:8082/formflow/user/form/moveToTrash/{id}  (patch) id is form-id
- get all trash form -- http://localhost:8082/formflow/user/form/trash  (get)
- restore form from trash -- http://localhost:8082/formflow/user/form/restoreFromTrash/{id}  (patch) id is form-id
- update form Details by id -- http://localhost:8082/formflow/user/updateForm/{id}  (put) id is form-id

### public APIs--for form Data
- get form by id -- http://localhost:8082/formflow/public/form/{id}  (get) id is form-id

### Admin APIs--
#### (get Access Token and Resfresh Token from login API and use it in header for below APIs)
- get all form -- http://localhost:8082/formflow/admin/getAllForms  (get)
- get all User -- http://localhost:8082/formflow/admin/getAllUsers  (get)

### Response APIs--
#### not authenticated -- anyone can access these APIs without authentication
- post respose -- http://localhost:8082/formflow/api/responses  (post)
- get form response by formId -- http://localhost:8082/formflow/api/responsesget/{formId} (get)
