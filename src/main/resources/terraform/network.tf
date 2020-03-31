resource "aws_internet_gateway" "cratekube-igw" {
  vpc_id = aws_vpc.cratekube-vpc.id

  tags = {
    Name = "cratekube-igw"
  }
}

resource "aws_route_table" "cratekube-crt" {
  vpc_id = aws_vpc.cratekube-vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.cratekube-igw.id
  }

  tags = {
    Name = "cratekube-crt"
  }
}

resource "aws_route_table_association" "cratekube-crta-subnet-1" {
  subnet_id = aws_subnet.cratekube-subnet-1.id
  route_table_id = aws_route_table.cratekube-crt.id
}

resource "aws_security_group" "cratekube-ssh-sg" {
  vpc_id = aws_vpc.cratekube-vpc.id

  egress {
    from_port = 0
    to_port = 0
    protocol = -1
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    // TODO - determine what the cidr block should be
    // This needs to be changed before production deployment
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "cratekube-ssh-sg"
  }
}
