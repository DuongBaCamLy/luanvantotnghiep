import http from "k6/http";
import { check, sleep } from "k6";


export const options = {


    scenarios:{


        curriculum_users:{


            executor:"constant-vus",

            vus:50,

            duration:"2m"


        }


    },


    thresholds:{


        http_req_duration:[
            "p(95)<3000"
        ],


        http_req_failed:[
            "rate<0.01"
        ]


    }


};



const BASE_URL =
"http://localhost:8080";



export default function(){


    const response =
        http.get(
            BASE_URL +
            "/api/dashboard/heatmap"
        );


    check(response,{


        "HTTP success":
            r=>r.status>=200 &&
               r.status<400,


        "response < 3s":
            r=>r.timings.duration < 3000


    });



    sleep(1);

}