package main

import (
	"fmt"
	"sync"
	"time"
)

type City struct {
	name string
}

type Route struct {
	from        *City
	to          *City
	ticketPrice int
}

type Graph struct {
	cities  map[string]*City
	routes  map[string]map[string]*Route
	rwMutex sync.RWMutex
}

func NewGraph() *Graph {
	return &Graph{
		cities: make(map[string]*City),
		routes: make(map[string]map[string]*Route),
	}
}

func (g *Graph) AddCity(name string) {
	g.rwMutex.Lock()
	defer g.rwMutex.Unlock()

	g.cities[name] = &City{name: name}
}

func (g *Graph) RemoveCity(name string) {
	g.rwMutex.Lock()
	defer g.rwMutex.Unlock()

	delete(g.cities, name)
	for _, routeMap := range g.routes {
		delete(routeMap, name)
	}
	delete(g.routes, name)
}

func (g *Graph) AddRoute(fromName, toName string, price int) {
	g.rwMutex.Lock()
	defer g.rwMutex.Unlock()

	fromCity, ok1 := g.cities[fromName]
	toCity, ok2 := g.cities[toName]
	if !ok1 || !ok2 {
		return
	}

	if g.routes[fromName] == nil {
		g.routes[fromName] = make(map[string]*Route)
	}
	g.routes[fromName][toName] = &Route{from: fromCity, to: toCity, ticketPrice: price}

	if g.routes[toName] == nil {
		g.routes[toName] = make(map[string]*Route)
	}
	g.routes[toName][fromName] = &Route{from: toCity, to: fromCity, ticketPrice: price}
}

func (g *Graph) RemoveRoute(fromName, toName string) {
	g.rwMutex.Lock()
	defer g.rwMutex.Unlock()

	if _, exists := g.routes[fromName]; exists {
		delete(g.routes[fromName], toName)
	}
	if _, exists := g.routes[toName]; exists {
		delete(g.routes[toName], fromName)
	}
}

func (g *Graph) ChangeTicketPrice(fromName, toName string, price int) {
	g.rwMutex.Lock()
	defer g.rwMutex.Unlock()

	if g.routes[fromName] != nil && g.routes[fromName][toName] != nil {
		g.routes[fromName][toName].ticketPrice = price
	}
	if g.routes[toName] != nil && g.routes[toName][fromName] != nil {
		g.routes[toName][fromName].ticketPrice = price
	}
}

func (g *Graph) FindPath(fromName, toName string) ([]*Route, bool) {
	g.rwMutex.RLock()
	defer g.rwMutex.RUnlock()

	visited := make(map[string]bool)
	queue := [][]*Route{}

	if g.routes[fromName] == nil {
		return nil, false
	}

	for _, route := range g.routes[fromName] {
		queue = append(queue, []*Route{route})
	}

	for len(queue) > 0 {
		path := queue[0]
		queue = queue[1:]

		lastCity := path[len(path)-1].to.name
		if lastCity == toName {
			return path, true
		}

		if visited[lastCity] {
			continue
		}
		visited[lastCity] = true

		for _, route := range g.routes[lastCity] {
			newPath := append(path[:], route)
			queue = append(queue, newPath)
		}
	}

	return nil, false
}

func populateGraph(g *Graph) {
	g.AddCity("Kyiv")
	g.AddCity("Lviv")
	g.AddCity("Odesa")
	g.AddCity("Dnipro")

	g.AddRoute("Kyiv", "Lviv", 200)
	g.AddRoute("Kyiv", "Odesa", 150)
	g.AddRoute("Lviv", "Dnipro", 250)
}

func main() {
	graph := NewGraph()
	populateGraph(graph)

	// Потік зміни ціни квитка
	go func() {
		for {
			fmt.Println("[ПотікЗміниЦіни] Змінюємо ціну квитка з Києва до Львова на 300.")
			graph.ChangeTicketPrice("Kyiv", "Lviv", 300)
			time.Sleep(1 * time.Second)
		}
	}()

	// Потік додавання/видалення рейсів
	go func() {
		for {
			fmt.Println("[ПотікРейсів] Додаємо рейс з Києва до Дніпра.")
			graph.AddRoute("Kyiv", "Dnipro", 220)
			time.Sleep(2 * time.Second)
			fmt.Println("[ПотікРейсів] Видаляємо рейс з Києва до Дніпра.")
			graph.RemoveRoute("Kyiv", "Dnipro")
			time.Sleep(2 * time.Second)
		}
	}()

	// Потік додавання/видалення міст
	go func() {
		for {
			fmt.Println("[ПотікМіст] Додаємо місто Харків.")
			graph.AddCity("Kharkiv")
			time.Sleep(3 * time.Second)
			fmt.Println("[ПотікМіст] Видаляємо місто Харків.")
			graph.RemoveCity("Kharkiv")
			time.Sleep(3 * time.Second)
		}
	}()

	// Потік пошуку шляху між містами
	go func() {
		for {
			fmt.Println("[ПотікПошукуШляху] Шукаємо шлях з Києва до Дніпра.")
			path, exists := graph.FindPath("Kyiv", "Dnipro")
			if exists {
				fmt.Println("[ПотікПошукуШляху] Шлях знайдено:")
				for _, route := range path {
					fmt.Println("[ПотікПошукуШляху] ", route.from.name, "->", route.to.name, "за ціною:", route.ticketPrice)
				}
			} else {
				fmt.Println("[ПотікПошукуШляху] Шлях не знайдено.")
			}
			time.Sleep(1 * time.Second)
		}
	}()

	// Блокуємо головний потік, дозволяючи іншим потокам працювати безперервно
	select {}
}
