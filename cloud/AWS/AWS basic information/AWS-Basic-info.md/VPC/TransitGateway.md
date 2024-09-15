# Transit Gateway
## Overivew
AWS의 네트워크는 다중 VPC들이 복잡하게 얽혀있는 토폴로지를 가집니다.
- VPC 여러개를 피어링으로 전부 연결하고, VPN과 Direct Connect를 구축하여 여러 VPC를 연결하는 형태기 때문

따라서. AWS는 해당 문제를 해결하기 위해 Transit Gateway를 만들었습니다.

Transit Gateway를 사용하게 되면, 여러 VPC가 하나의 Transit Gateway를 통해 연결되게 되며, AWS Direct Connect Gateway, Site to Site VPN 등 과의 연결또한 가능합니다.
- 모든 네트워크의 통로를 Transit Gateway로 공통시킬 수 있습니다.

리전 리소스기 때문에, 리전간에 작동합니다.

