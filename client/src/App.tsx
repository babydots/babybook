import React, {useEffect, useState} from 'react';
import './App.css';
import {Button, Col, Container, Form, FormGroup, Input, Label, Row, Spinner, Card, CardImg, CardImgOverlay} from "reactstrap";
import {FaCheck} from "react-icons/all";
import {AsyncTypeahead} from "react-bootstrap-typeahead";

type IExampleBook = {
    title: string
    pages: string[]
}

const exampleBooks: IExampleBook[] = [
    {
        title: "Space",
        pages: ["Earth", "Sun", "Moon", "Mercury (planet)", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"]
    },
    {title: "Animals", pages: ["Elephant", "Turtle", "Sheep", "Dog", "Cat", "Snake", "Cow", "Chicken", "Horse"]},
    {title: "Machines", pages: ["Car", "Excavator", "Truck", "Bus"]},
    // {title: "Things", pages: ["Magnet",]},
    // {title: "Greek Gods", pages: ["Aphrodite", "Apollo", "Ares", "Artemis", "Athena", "Demeter", "Dionysus", "Hades", "Hephaestus", "Hera", "Hermes", "Hestia", "Poseidon", "Zeus",]}

]

function getBaseUrl() {
    if (document.location.host === "localhost:3000") {
        return "http://localhost:8080"
    } else {
        return ""
    }
}

function App() {

    const [title, setTitle] = useState<string>('')
    const [pages, setPages] = useState<string[]>([''])
    const [pagesInPrep, setPagesInPrep] = useState<string[]>([])
    const [pagesPrepared, setPagesPrepared] = useState<IPageData[]>([])

    const onSubmit = () => {
        const params = new URLSearchParams()
        params.append("title", title)
        params.append("pages", pages.filter(p => p.trim().length > 0).join("_"))
        document.location = getBaseUrl() + "/book?" + params.toString()
    }

    const onClear = () => {
        setTitle('')
        setPages([''])
    }

    const onExampleSelected = (example: IExampleBook) => {
        setTitle(example.title)
        setPages([...example.pages, ''])
    }

    const onUpdatePage = async (index: number, value: string) => {

        // Last page has been populated, create a new blank page at the end now.
        if (index == pages.length - 1 && value !== '') {
            setPages([...pages.slice(0, index), value, ''])
        } else {
            setPages([...pages.slice(0, index), value, ...pages.slice(index + 1)])
        }

        if (value !== '' && pagesPrepared.find(pageData => pageData.title === value) == null) {

            setPagesInPrep(pagesInPrep.concat([value]))

            const response = await fetch(`${getBaseUrl()}/wiki/page?title=${value}`)
            const pageData: IPageData = await response.json()

            setPagesPrepared(pagesPrepared.concat([pageData]));
            setPagesInPrep(pagesInPrep.filter(p => p !== value));

        }
    }

    return (
        <Container>
            <h1>Make your own picture book</h1>
            <p>
                For each page, tell us the title of an article
                on <a target="_blank" href="https://simple.wikipedia.org">Simple English Wikipedia</a>
            </p>
            <p>
                We will then fetch an image and a simple summary, then turn it into a beautiful PDF for you to download and print.
            </p>
            <p>
                Examples: <Examples onSelected={(example) => onExampleSelected(example)}/>
            </p>
            <Form onSubmit={event => {
                onSubmit();
                event.preventDefault()
            }}>
                <Row>
                    <Col sm={12} md={10} lg={6}>

                        <FormGroup>
                            <Label for="title">Book Title</Label>
                            <Row>
                                <Col sm={12} md={8}>
                                    <Input type="text" id="title" value={title}
                                           onChange={(event) => setTitle(event.target.value)}/>
                                </Col>
                            </Row>
                        </FormGroup>

                    </Col>
                </Row>
                <Row style={{marginBottom: '2rem'}}>
                    <Col sm={12} md={6}>
                        <Button
                            color="primary"
                            onClick={onSubmit}
                            style={{marginRight: '1rem'}}>
                            Download Book
                        </Button>
                        {title !== '' && pages.length > 0 && pages[0] !== '' &&
                            <Button
                                color="danger"
                                size="sm"
                                outline
                                onClick={onClear}>
                                Start again
                                </Button>}
                    </Col>
                </Row>
                {pages.map((page, i) => {
                    return (
                        <Row key={`page-${i}`}>
                            <Col sm={12} md={8} lg={4}>
                                <PageInput
                                    index={i}
                                    value={page}
                                    onChange={value => onUpdatePage(i, value)}
                                    isFetchingPage={page !== "" && pagesInPrep.indexOf(page) !== -1}
                                    pageData={pagesPrepared.find(pageData => pageData.title === page)}
                                />
                            </Col>
                        </Row>
                    );
                })}
            </Form>
        </Container>
    );
}

type IExamplesProps = {
    onSelected: (example: IExampleBook) => void
}

function Examples(props: IExamplesProps) {
    return (
        <React.Fragment>
            {exampleBooks.map(example =>
                <Button
                    color="link"
                    key={example.title}
                    onClick={() => props.onSelected(example)}>
                    {example.title}
                </Button>)}
        </React.Fragment>
    )
}

type ISearchResults = {
    results: ISearchResult[]
}

type ISearchResult = {
    title: string
    snippet: string
    pageid: number
}

type IPageData = {
    title: string
    image: string
    text: string
}

type IPageInputProps = {
    index: number
    value: string
    onChange: (value: string) => void
    isFetchingPage: boolean
    pageData: IPageData|undefined
}

function PageInput(props: IPageInputProps) {
    const id = `page${props.index}`

    return (
        <>
            <FormGroup>
                <Label for={id}>
                    Page {props.index + 1}
                    {props.isFetchingPage && <IconWrapper><Spinner size="sm" color="secondary">&nbsp;</Spinner></IconWrapper>}
                    {props.pageData !== undefined && <IconWrapper><FaCheck color="#3b3" /></IconWrapper>}
                </Label>
                <div style={{position: "relative"}}>
                    <TitleSearch id={id} title={props.value} onChange={props.onChange} />
                </div>
            </FormGroup>
            {props.pageData != null &&
                <Card>
                    <CardImg src={`/wiki/image/${props.pageData.title}/${props.pageData.image}`} />
                    <CardImgOverlay>
                        <span className="img-text-overlay card-title">{props.pageData.title}</span>
                    </CardImgOverlay>
                    <CardImgOverlay className="d-flex flex-column justify-content-end">
                        <p className="img-text-overlay">{props.pageData.text}</p>
                    </CardImgOverlay>
                </Card>}
        </>
    )
}

const IconWrapper = (props: {children: any}) =>
    <div className="icon-wrapper" style={{position: "absolute", top: "4px", bottom: "4px", right: "4px", display: "inline-block", width: "16px", height: "16px"}}>
        {props.children}
    </div>


type ITitleSearchProps = {
    id: string
    title: string
    onChange: (value: string) => void
}

const TitleSearch = (props: ITitleSearchProps) => {
    const [isLoading, setIsLoading] = useState(false);
    const [options, setOptions] = useState<string[]>([]);

    useEffect(() => {
        setOptions(props.title === '' ? [] : [props.title])
    }, [props.title])

    const handleSearch = async (query: string) => {
        setIsLoading(true);

        const response = await fetch(`${getBaseUrl()}/wiki/search?q=${query}`)
        const results: ISearchResults = await response.json()

        setOptions(results.results.map(r => r.title));
        setIsLoading(false);
    };

    // Bypass client-side filtering by returning `true`. Results are already
    // filtered by the search endpoint, so no need to do it again.
    const filterBy = () => true;

    return (
        <AsyncTypeahead
            id={props.id}
            filterBy={filterBy}
            isLoading={isLoading}
            minLength={3}
            onSearch={handleSearch}
            options={options}
            useCache={false}
            selected={props.title === '' ? [] : [props.title]}
            onChange={(values) => props.onChange(values.length === 0 ? '' : values[0])}
            placeholder="Search Wikipedia..."
            renderMenuItemChildren={(option, props) => (
                <span>{option}</span>
            )}
        />
    );
};

export default App;
